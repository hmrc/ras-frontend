/*
 * Copyright 2018 HM Revenue & Customs
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

import config.{FrontendAuthConnector, RasContext, RasContextImpl}
import connectors.{ResidencyStatusAPIConnector, UserDetailsConnector}
import play.api.mvc.{Action, AnyContent, Request}
import play.api.{Configuration, Environment, Logger, Play}
import uk.gov.hmrc.auth.core.AuthConnector
import forms.MemberDateOfBirthForm.form
import metrics.Metrics
import models.{MemberDetails, ResidencyStatus, ResidencyStatusResult}
import services.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future

object MemberDOBController extends MemberDOBController {
  // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  override val residencyStatusAPIConnector = ResidencyStatusAPIConnector
  override val auditService: AuditService = AuditService
  // $COVERAGE-ON$
}

trait MemberDOBController extends RasController with PageFlowController {

  implicit val context: RasContext = RasContextImpl
  val residencyStatusAPIConnector : ResidencyStatusAPIConnector
  val auditService: AuditService

  val SCOTTISH = "scotResident"
  val NON_SCOTTISH = "otherUKResident"
  val RAS = "ras"
  val NO_MATCH = "noMatch"
  var firstName = ""

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              Ok(views.html.member_dob(form.fill(session.dateOfBirth),name))
            case _ => Ok(views.html.member_dob(form, firstName))
          }
        case Left(resp) =>
          Logger.error("[DobController][get] user Not authorised")
          resp
      }
  }

  def post = Action.async {
    implicit request =>
    isAuthorised.flatMap{
      case Right(userId) =>
        form.bindFromRequest.fold(
        formWithErrors => {
          Logger.error("[DobController][post] Invalid form field passed")
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              BadRequest(views.html.member_dob(formWithErrors, name))
            case _ =>
              BadRequest(views.html.member_dob(formWithErrors, Messages("member")))
          }
        },
        dateOfBirth => {
          val timer = Metrics.responseTimer.time()
          sessionService.cacheDob(dateOfBirth) flatMap {
            case Some(session) =>

              val memberDetails = MemberDetails(session.name, session.nino.nino, session.dateOfBirth.dateOfBirth)

              residencyStatusAPIConnector.getResidencyStatus(memberDetails).map { rasResponse =>

                val formattedName = session.name.firstName + " " + session.name.lastName
                val formattedDob = dateOfBirth.dateOfBirth.asLocalDate.toString("d MMMM yyyy")
                val cyResidencyStatus = extractResidencyStatus(rasResponse.currentYearResidencyStatus)
                val nyResidencyStatus:Option[String] =
                  if (rasResponse.nextYearForecastResidencyStatus.nonEmpty)
                    Some(extractResidencyStatus(rasResponse.nextYearForecastResidencyStatus.get))
                  else
                    None

                if (cyResidencyStatus.isEmpty) {
                  Logger.error("[DobController][post] An unknown residency status was returned")
                  Redirect(routes.ErrorController.renderGlobalErrorPage())
                }
                else {
                  Logger.info("[DobController][post] Match found")

                  timer.stop()

                  val residencyStatusResult =
                    ResidencyStatusResult(
                      cyResidencyStatus, nyResidencyStatus,
                      TaxYearResolver.currentTaxYear.toString,
                      (TaxYearResolver.currentTaxYear + 1).toString,
                      formattedName, formattedDob, memberDetails.nino)

                  sessionService.cacheResidencyStatusResult(residencyStatusResult)

                  auditResponse(failureReason = None, nino = Some(memberDetails.nino),
                    residencyStatus = Some(rasResponse),
                    userId = userId)

                  Redirect(routes.ResultsController.matchFound())
                }
              }.recover {
                case e: Upstream4xxResponse if e.upstreamResponseCode == FORBIDDEN =>
                  auditResponse(failureReason = Some("MATCHING_FAILED"), nino = Some(memberDetails.nino),
                    residencyStatus = None, userId = userId)
                  Logger.info("[DobController][getResult] No match found from customer matching")
                  timer.stop()
                  Redirect(routes.ResultsController.noMatchFound())
                case e: Throwable =>
                  auditResponse(failureReason = Some("INTERNAL_SERVER_ERROR"), nino = Some(memberDetails.nino),
                    residencyStatus = None, userId = userId)
                  Logger.error(s"[DobController][getResult] Customer Matching failed: ${e.getMessage}")
                  Redirect(routes.ErrorController.renderGlobalErrorPage())
              }
            case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
          }
        }
      )
      case Left(res) => res
    }
  }

  def back = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userInfo) =>
          sessionService.fetchRasSession() map {
            case Some(session) => previousPage("MemberDOBController")
            case _ => Redirect(routes.ErrorController.renderGlobalErrorPage())
          }
        case Left(res) => res
      }
  }

  private def extractResidencyStatus(residencyStatus: String) : String = {
    if(residencyStatus == SCOTTISH)
      Messages("scottish.taxpayer")
    else if(residencyStatus == NON_SCOTTISH)
      Messages("non.scottish.taxpayer")
    else
      ""
  }

  /**
    * Audits the response, if failure reason is None then residencyStatus is Some (sucess) and vice versa (failure).
    * @param failureReason Optional message, present if the journey failed, else not
    * @param nino Optional user identifier, present if the customer-matching-cache call was a success, else not
    * @param residencyStatus Optional status object returned from the HoD, present if the journey succeeded, else not
    * @param userId Identifies the user which made the request
    * @param request Object containing request made by the user
    * @param hc Headers
    */
  private def auditResponse(failureReason: Option[String], nino: Option[String],
                            residencyStatus: Option[ResidencyStatus], userId: String)
                           (implicit request: Request[AnyContent], hc: HeaderCarrier): Unit = {

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

    auditService.audit(auditType = "ReliefAtSourceResidency",
      path = request.path,
      auditData = auditDataMap ++ Map("userIdentifier" -> userId, "requestSource" -> "FE_SINGLE") ++ ninoMap
    )
  }

}
