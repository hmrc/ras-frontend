/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import connectors.ResidencyStatusAPIConnector
import metrics.Metrics
import models._
import play.api.Logging
import play.api.http.Status.FORBIDDEN
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import services.{AuditService, SessionService, TaxYearResolver}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

trait RasResidencyCheckerController extends RasController with AuditService with Logging {

  val residencyStatusAPIConnector: ResidencyStatusAPIConnector
  val apiVersion: ApiVersion
	val sessionService: SessionService

	val SCOTTISH = "scotResident"
	val WELSH = "welshResident"
	val OTHER_UK = "otherUKResident"

  def submitResidencyStatus(session: RasSession, userId: String)(implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    val timer = Metrics.responseTimer.time()

    if (session.name.hasAValue && session.nino.hasAValue && session.dateOfBirth.hasAValue) {

      val memberDetails = MemberDetails(session.name, session.nino.nino, session.dateOfBirth.dateOfBirth)

      residencyStatusAPIConnector.getResidencyStatus(memberDetails).flatMap { rasResponse =>
        val formattedName = session.name.firstName + " " + session.name.lastName
        val formattedDob = session.dateOfBirth.dateOfBirth.asLocalDate.toString("d MMMM yyyy")
        val cyResidencyStatus = extractResidencyStatus(rasResponse.currentYearResidencyStatus)
        val nyResidencyStatus: Option[String] =
          rasResponse.nextYearForecastResidencyStatus.map(extractResidencyStatus)
        if (cyResidencyStatus.isEmpty) {
          logger.error("[RasResidencyCheckerController][post] An unknown residency status was returned")
          Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
        }
        else {
          logger.info("[RasResidencyCheckerController][post] Match found")

          timer.stop()

          val residencyStatusResult =
            ResidencyStatusResult(
              cyResidencyStatus, nyResidencyStatus,
              TaxYearResolver.currentTaxYear.toString,
              (TaxYearResolver.currentTaxYear + 1).toString,
              formattedName, formattedDob, memberDetails.nino)
          auditResponse(failureReason = None, nino = Some(memberDetails.nino),
            residencyStatus = Some(rasResponse),
            userId = userId)

          for {
            _ <- sessionService.cacheResidencyStatusResult(residencyStatusResult)
          }
            yield {
              Redirect(routes.ResultsController.matchFound)
            }
        }
      }.recover {
        case UpstreamErrorResponse.WithStatusCode(FORBIDDEN) =>
          auditResponse(failureReason = Some("MATCHING_FAILED"), nino = Some(memberDetails.nino),
            residencyStatus = None, userId = userId)
          logger.info("[RasResidencyCheckerController][getResult] No match found from customer matching")
          timer.stop()
          Redirect(routes.ResultsController.noMatchFound)
        case e: Throwable =>
          auditResponse(failureReason = Some("INTERNAL_SERVER_ERROR"), nino = Some(memberDetails.nino),
            residencyStatus = None, userId = userId)
          logger.error(s"[RasResidencyCheckerController][getResult] Customer Matching failed: ${e.getMessage}")
          Redirect(routes.ErrorController.renderGlobalErrorPage)
      }
    } else {
      sessionService.resetRasSession().map { _ =>
        Redirect(routes.ErrorController.startAtStart)
      }
    }
  }

  private[controllers] def extractResidencyStatus(residencyStatus: String): String = {
    (residencyStatus, apiVersion) match {
      case (SCOTTISH, _) => "Scotland"
      case (WELSH, ApiV2_0) => "Wales"
      case (OTHER_UK, ApiV1_0) => "England, Northern Ireland or Wales"
      case (OTHER_UK, ApiV2_0) => "England, Northern Ireland or Wales"
      case _ => ""
    }
  }

  /**
    * Audits the response, if failure reason is None then residencyStatus is Some (sucess) and vice versa (failure).
    *
    * @param failureReason   Optional message, present if the journey failed, else not
    * @param nino            Optional user identifier, present if the customer-matching-cache call was a success, else not
    * @param residencyStatus Optional status object returned from the HoD, present if the journey succeeded, else not
    * @param userId          Identifies the user which made the request
    * @param request         Object containing request made by the user
    * @param hc              Headers
    */
  private def auditResponse(failureReason: Option[String], nino: Option[String],
                            residencyStatus: Option[ResidencyStatus], userId: String)
                           (implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Unit = {

    val ninoMap: Map[String, String] = nino.map(nino => Map("nino" -> nino)).getOrElse(Map())
    val nextYearStatusMap: Map[String, String] = if (residencyStatus.nonEmpty) residencyStatus.get.nextYearForecastResidencyStatus
      .map(nextYear => Map("NextCYStatus" -> nextYear)).getOrElse(Map())
    else Map()
    val auditDataMap: Map[String, String] = failureReason.map(reason => Map("successfulLookup" -> "false",
      "reason" -> reason)).
      getOrElse(Map(
        "successfulLookup" -> "true",
        "CYStatus" -> residencyStatus.get.currentYearResidencyStatus
      ) ++ nextYearStatusMap)

    audit(auditType = "ReliefAtSourceResidency",
      path = request.path,
      auditData = auditDataMap ++ Map("userIdentifier" -> userId, "requestSource" -> "FE_SINGLE") ++ ninoMap
    )
  }

	def getFullName()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
		sessionService.fetchRasSession() map {
			case Some(session) => session.name.firstName.capitalize + " " + session.name.lastName.capitalize
			case _ => "member"
		}
	}
}
