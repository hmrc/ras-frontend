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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import config.ApplicationConfig
import connectors.ResidencyStatusAPIConnector
import models.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.wordspec.AnyWordSpec
import play.api.Logger
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{SEE_OTHER, defaultAwaitTimeout, status}
import services.SessionCacheService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.AuditExtensions.*
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import utils.RasTestHelper

import scala.concurrent.Future

class RasResidencyCheckerControllerSpec
    extends AnyWordSpec with RasTestHelper with LogCapturing with BeforeAndAfterEach {

  def configureRasResidencyCheckerController(): RasResidencyCheckerController =
    new RasResidencyCheckerController {
      override val authConnector: AuthConnector                             = mockAuthConnector
      override val auditConnector: DefaultAuditConnector                    = mockAuditConnector
      override val sessionService: SessionCacheService                      = mockRasSessionCacheService
      override val residencyStatusAPIConnector: ResidencyStatusAPIConnector = mockResidencyStatusAPIConnector
      override val appConfig: ApplicationConfig                             = mockAppConfig
    }

  private val controller = configureRasResidencyCheckerController()

  private val controllerLogger = Logger(controller.getClass)

  private val tag         = "[RasResidencyCheckerController][submitResidencyStatus]"
  private val userId      = "A123456"
  private val nino        = "AB123456C"
  private val requestPath = "/relief-at-source/member-date-of-birth"

  private val fullSession = RasSession(
    MemberName("John", "Smith"),
    MemberNino(nino),
    MemberDateOfBirth(RasDate(Some("1"), Some("2"), Some("1990")))
  )

  given request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", requestPath)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector, mockResidencyStatusAPIConnector, mockRasSessionCacheService)
    when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
    when(mockRasSessionCacheService.cacheResidencyStatusResult(any())(any()))
      .thenReturn(Future.successful(Some(fullSession)))
    when(mockRasSessionCacheService.resetRasSession()(any()))
      .thenReturn(Future.successful(Some(RasSession.cleanSession)))
  }

  private def connectorReturns(result: Future[ResidencyStatus]): Unit =
    when(mockResidencyStatusAPIConnector.getResidencyStatus(any(), any())(any(), any())).thenReturn(result)

  private def auditedDetail(): Map[String, String] = {
    val captor = ArgumentCaptor.forClass(classOf[DataEvent])
    verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

    val event = captor.getValue
    event.auditSource  shouldBe "ras-frontend"
    event.auditType    shouldBe "ReliefAtSourceResidency"
    event.tags("path") shouldBe requestPath
    event.detail
  }

  private val commonAuditFields =
    hc.toAuditDetails() ++ Map("userIdentifier" -> userId, "requestSource" -> "FE_SINGLE", "nino" -> nino)

  private def logLines(logs: List[ILoggingEvent]): List[(Level, String)] =
    logs.map(event => event.getLevel -> event.getFormattedMessage)

  "RasResidencyCheckerController extractResidencyStatus" when {
    "version 2.0 of the API is used" must {
      "extract the result into the correct messages" in {
        controller.extractResidencyStatus(SCOTTISH) shouldBe "Scotland"
        controller.extractResidencyStatus(WELSH)    shouldBe "Wales"
        controller.extractResidencyStatus(OTHER_UK) shouldBe "England, Northern Ireland or Wales"
        controller.extractResidencyStatus("")       shouldBe ""
      }
    }
  }

  "RasResidencyCheckerController submitResidencyStatus" when {

    "the lookup succeeds" must {
      "redirect to match found, audit the success and log the match" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("/member-residency-status")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "true",
            "httpStatus"       -> "200",
            "CYStatus"         -> SCOTTISH,
            "NextCYStatus"     -> OTHER_UK
          )

          logLines(logs) shouldBe List(
            Level.INFO -> s"$tag Match found userId=$userId"
          )
        }

      "not audit NextCYStatus when there is no next year status" in {
        connectorReturns(Future.successful(ResidencyStatus(SCOTTISH, None)))

        status(controller.submitResidencyStatus(fullSession, userId)) shouldBe SEE_OTHER

        auditedDetail() shouldBe commonAuditFields ++ Map(
          "successfulLookup" -> "true",
          "httpStatus"       -> "200",
          "CYStatus"         -> SCOTTISH
        )
      }
    }

    "the member is not matched (403)" must {
      "redirect to no match found and audit MATCHING_FAILED without logging again" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.failed(ConnectorFailure(FailureReason.MATCHING_FAILED, Some(403))))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("/no-residency-status-displayed")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "false",
            "reason"           -> "MATCHING_FAILED",
            "httpStatus"       -> "403"
          )

          // connector has already logged the failure
          logs shouldBe empty
        }
    }

    "the connector fails with any other ConnectorFailure" must {

      "redirect to the global error page and audit TOO_MANY_REQUESTS without logging again" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.failed(ConnectorFailure(FailureReason.TOO_MANY_REQUESTS, Some(429))))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("global-error")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "false",
            "reason"           -> "TOO_MANY_REQUESTS",
            "httpStatus"       -> "429"
          )

          logs shouldBe empty
        }

      "redirect to the global error page and audit SERVICE_UNAVAILABLE without logging again" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.failed(ConnectorFailure(FailureReason.SERVICE_UNAVAILABLE, Some(503))))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("global-error")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "false",
            "reason"           -> "SERVICE_UNAVAILABLE",
            "httpStatus"       -> "503"
          )

          logs shouldBe empty
        }

      "redirect to the global error page and audit INTERNAL_SERVER_ERROR without logging again" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.failed(ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, Some(500))))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("global-error")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "false",
            "reason"           -> "INTERNAL_SERVER_ERROR",
            "httpStatus"       -> "500"
          )

          logs shouldBe empty
        }

      "redirect to the global error page and audit TIMEOUT without an httpStatus or logging again" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.failed(ConnectorFailure(FailureReason.TIMEOUT)))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("global-error")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "false",
            "reason"           -> "TIMEOUT"
          )

          logs shouldBe empty
        }

      "include parseErrors in the audit for an invalid response" in {
        val parseErrors = "/currentYearResidencyStatus: error.path.missing"
        connectorReturns(
          Future.failed(ConnectorFailure(FailureReason.INVALID_RESPONSE, Some(200), parseErrors = Some(parseErrors)))
        )

        status(controller.submitResidencyStatus(fullSession, userId)) shouldBe SEE_OTHER

        auditedDetail() shouldBe commonAuditFields ++ Map(
          "successfulLookup" -> "false",
          "reason"           -> "INVALID_RESPONSE",
          "httpStatus"       -> "200",
          "parseErrors"      -> parseErrors
        )
      }
    }

    "the API returns an unknown residency status" must {
      "redirect to the global error page, audit UNKNOWN_RESIDENCY_STATUS and log the returned status" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.successful(ResidencyStatus("notAStatus", None)))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("global-error")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "false",
            "reason"           -> "UNKNOWN_RESIDENCY_STATUS",
            "httpStatus"       -> "200"
          )

          logLines(logs) shouldBe List(
            Level.ERROR -> s"$tag Unknown residency status returned currentYearResidencyStatus=notAStatus userId=$userId"
          )

          verify(mockRasSessionCacheService, never).cacheResidencyStatusResult(any())(any())
        }
    }

    "an unexpected exception occurs" must {
      "redirect to the global error page, audit INTERNAL_SERVER_ERROR and log the exception type" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          connectorReturns(Future.failed(new RuntimeException("boom")))

          val result = controller.submitResidencyStatus(fullSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("global-error")

          auditedDetail() shouldBe commonAuditFields ++ Map(
            "successfulLookup" -> "false",
            "reason"           -> "INTERNAL_SERVER_ERROR"
          )

          logLines(logs) shouldBe List(
            Level.ERROR -> s"$tag Unexpected exception=RuntimeException userId=$userId"
          )
        }
    }

    "the session is missing mandatory data" must {
      "redirect to startAtStart, log the missing fields and not call the API or audit" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          val result = controller.submitResidencyStatus(RasSession.cleanSession, userId)

          status(result)         shouldBe SEE_OTHER
          redirectLocation(result) should include("service-start-at-start")

          logLines(logs) shouldBe List(
            Level.WARN -> s"$tag Missing mandatory session data missingFields=name,nino,dateOfBirth userId=$userId"
          )

          verify(mockResidencyStatusAPIConnector, never).getResidencyStatus(any(), any())(any(), any())
          verify(mockAuditConnector, never).sendEvent(any())(any(), any())
        }

      "only list the fields that are actually missing" in
        withCaptureOfLoggingFrom(controllerLogger) { logs =>
          val noNinoSession = fullSession.copy(nino = MemberNino(""))

          status(controller.submitResidencyStatus(noNinoSession, userId)) shouldBe SEE_OTHER

          logLines(logs) shouldBe List(
            Level.WARN -> s"$tag Missing mandatory session data missingFields=nino userId=$userId"
          )
        }
    }
  }

}
