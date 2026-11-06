/*
 * Copyright 2026 HM Revenue & Customs
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
import models.*
import models.FailureReason.{INTERNAL_SERVER_ERROR, MATCHING_FAILED, UNKNOWN_RESIDENCY_STATUS}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import services.{AuditService, ResidencyAuditData, SessionCacheService, TaxYearResolver}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.{ExecutionContext, Future}

trait RasResidencyCheckerController extends RasController with AuditService with Logging {

  val residencyStatusAPIConnector: ResidencyStatusAPIConnector
  val sessionService: SessionCacheService

  val SCOTTISH = "scotResident"
  val WELSH    = "welshResident"
  val OTHER_UK = "otherUKResident"

  private val tag = "[RasResidencyCheckerController][submitResidencyStatus]"

  def submitResidencyStatus(session: RasSession, userId: String)(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] =
    if (session.name.hasAValue && session.nino.hasAValue && session.dateOfBirth.hasAValue) {

      val timer         = Metrics.responseTimer.time()
      val memberDetails = MemberDetails(session.name, session.nino.nino, session.dateOfBirth.dateOfBirth)

      residencyStatusAPIConnector
        .getResidencyStatus(memberDetails, userId)
        .andThen(_ => timer.stop())
        .flatMap(rasResponse => handleResidencyStatus(session, memberDetails, rasResponse, userId))
        .recover {
          case failure: ConnectorFailure if failure.reason == MATCHING_FAILED =>
            auditLookup(ResidencyAuditData.failure(userId, memberDetails.nino, failure))
            Redirect(routes.ResultsController.noMatchFound)

          case failure: ConnectorFailure =>
            auditLookup(ResidencyAuditData.failure(userId, memberDetails.nino, failure))
            Redirect(routes.ErrorController.renderGlobalErrorPage)

          case e: Throwable =>
            auditLookup(ResidencyAuditData.failure(userId, memberDetails.nino, ConnectorFailure(INTERNAL_SERVER_ERROR)))
            logger.error(s"$tag Unexpected exception=${e.getClass.getSimpleName} userId=$userId", e)
            Redirect(routes.ErrorController.renderGlobalErrorPage)
        }

    } else {
      val missingFields = Seq(
        "name"        -> session.name.hasAValue,
        "nino"        -> session.nino.hasAValue,
        "dateOfBirth" -> session.dateOfBirth.hasAValue
      ).collect { case (field, false) => field }.mkString(",")

      logger.warn(s"$tag Missing mandatory session data missingFields=$missingFields userId=$userId")

      sessionService.resetRasSession().map(_ => Redirect(routes.ErrorController.startAtStart))
    }

  private def handleResidencyStatus(
    session: RasSession,
    memberDetails: MemberDetails,
    rasResponse: ResidencyStatus,
    userId: String
  )(implicit request: Request[AnyContent], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val cyResidencyStatus = extractResidencyStatus(rasResponse.currentYearResidencyStatus)

    if (cyResidencyStatus.isEmpty) {
      logger.error(
        s"$tag Unknown residency status returned currentYearResidencyStatus=${rasResponse.currentYearResidencyStatus} userId=$userId"
      )

      auditLookup(
        ResidencyAuditData.failure(userId, memberDetails.nino, ConnectorFailure(UNKNOWN_RESIDENCY_STATUS, Some(200)))
      )

      Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
    } else {
      logger.info(s"$tag Match found userId=$userId")

      val formattedName = session.name.firstName + " " + session.name.lastName

      val formattedDob = session.dateOfBirth.dateOfBirth.asLocalDate.format(
        DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(Locale.UK)
      )

      val nyResidencyStatus: Option[String] = rasResponse.nextYearForecastResidencyStatus.map(extractResidencyStatus)

      val residencyStatusResult = ResidencyStatusResult(
        cyResidencyStatus,
        nyResidencyStatus,
        TaxYearResolver.currentTaxYear.toString,
        TaxYearResolver.nextTaxYear.toString,
        formattedName,
        formattedDob,
        memberDetails.nino
      )

      sessionService
        .cacheResidencyStatusResult(residencyStatusResult)
        .map { _ =>
          auditLookup(ResidencyAuditData.success(userId, memberDetails.nino, rasResponse))
          Redirect(routes.ResultsController.matchFound)
        }
        .recover { case e =>
          // the API returned a status and audited it, but the frontend couldn't store it for the results page
          auditLookup(
            ResidencyAuditData.failure(userId, memberDetails.nino, ConnectorFailure(INTERNAL_SERVER_ERROR, Some(200)))
          )
          logger.error(
            s"$tag Residency status could not be cached exception=${e.getClass.getSimpleName} userId=$userId"
          )
          Redirect(routes.ErrorController.renderGlobalErrorPage)
        }
    }
  }

  private[controllers] def extractResidencyStatus(residencyStatus: String): String =
    residencyStatus match {
      case SCOTTISH => "Scotland"
      case WELSH    => "Wales"
      case OTHER_UK => "England, Northern Ireland or Wales"
      case _        => ""
    }

  private def auditLookup(auditData: Map[String, String])(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Unit =
    audit(auditType = "ReliefAtSourceResidency", path = request.path, auditData = auditData)

  def getFullName()(implicit ec: ExecutionContext, request: Request[?]): Future[String] =
    sessionService.fetchRasSession() map {
      case Some(session) => session.name.firstName.capitalize + " " + session.name.lastName.capitalize
      case _             => "member"
    }

}
