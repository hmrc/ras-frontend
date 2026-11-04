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

package connectors

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlPathEqualTo}
import config.ApplicationConfig
import models.upscan.{UpscanFileReference, UpscanInitiateResponse}
import models.{ConnectorFailure, FailureReason}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Logger
import play.api.http.Status.{BAD_REQUEST, OK, SERVICE_UNAVAILABLE}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import utils.RasTestHelper

class UpscanInitiateConnectorSpec extends AnyWordSpec with Matchers with RasTestHelper with LogCapturing {

  val httpClient: HttpClientV2           = fakeApplication.injector.instanceOf[HttpClientV2]
  val connector: UpscanInitiateConnector = new UpscanInitiateConnector(httpClient, mockAppConfig)

  private val connectorLogger = Logger(classOf[UpscanInitiateConnector])

  private val tag         = "[UpscanInitiateConnector][initiateUpscan]"
  private val userId      = "A123456"
  private val initiateUrl = "/upscan/v2/initiate"
  private val callingLine = Level.INFO -> s"$tag Calling upscan-initiate userId=$userId"

  val upscanInitiateResponseJson: String =
    """
      |{
      |  "reference": "reference-1234",
      |  "uploadRequest": {
      |    "href": "downloadUrl",
      |    "fields": {
      |      "formKey": "formValue"
      |    }
      |  }
      |}
    """.stripMargin

  private def logLines(logs: List[ILoggingEvent]): List[(Level, String)] =
    logs.map(event => event.getLevel -> event.getFormattedMessage)

  private def initiate(): UpscanInitiateResponse =
    await(connector.initiateUpscan(userId, Some("successRedirectUrl"), Some("errorRedirectUrl")))

  "initiateUpscan" should {

    "return the upload details and log the call and success at info when upscan returns 200" in {
      setupMockPost(OK, upscanInitiateResponseJson, initiateUrl)

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        initiate() shouldBe UpscanInitiateResponse(
          UpscanFileReference("reference-1234"),
          "downloadUrl",
          Map("formKey" -> "formValue")
        )

        logLines(logs) shouldBe List(
          callingLine,
          Level.INFO -> s"$tag outcome=SUCCESS userId=$userId status=200 reference=reference-1234"
        )
      }
    }

    "fail with INTERNAL_SERVER_ERROR and log the actual status at error when upscan returns 400" in {
      setupMockPost(BAD_REQUEST, "{}", initiateUrl)

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](initiate())

        failure shouldBe ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, Some(BAD_REQUEST))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$tag Unexpected status code received from upscan-initiate " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=400")
        )
      }
    }

    "fail with INTERNAL_SERVER_ERROR and log the actual status at error when upscan returns 503" in {
      setupMockPost(SERVICE_UNAVAILABLE, "{}", initiateUrl)

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](initiate())

        failure shouldBe ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, Some(SERVICE_UNAVAILABLE))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$tag Unexpected status code received from upscan-initiate " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=503")
        )
      }
    }

    "fail with INVALID_RESPONSE, logging the JSON paths, when a 200 body is JSON in the wrong shape" in {
      setupMockPost(OK, """{"uploadRequest": {"href": "downloadUrl", "fields": {}}}""", initiateUrl)

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure     = intercept[ConnectorFailure](initiate())
        val parseErrors = "/reference: error.path.missing"

        failure shouldBe ConnectorFailure(FailureReason.INVALID_RESPONSE, Some(OK), parseErrors = Some(parseErrors))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$tag Upscan initiate response did not match expected format " +
            s"outcome=FAILURE reason=INVALID_RESPONSE userId=$userId status=200 parseErrors=$parseErrors")
        )
      }
    }

    "fail with PARSE_ERROR, without logging the body, when a 200 body is not JSON" in {
      // the parser's message would quote this value, so check it's not logged below
      setupMockPost(OK, """{"reference": reference1234}""", initiateUrl)

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](initiate())

        failure.copy(cause = None) shouldBe ConnectorFailure(FailureReason.PARSE_ERROR, Some(OK))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$tag Upscan initiate response was not valid json " +
            s"outcome=FAILURE reason=PARSE_ERROR userId=$userId status=200 cause=JsonParseException")
        )
      }
    }

    "fail with TIMEOUT and log at error when upscan doesn't respond in time" in {
      val shortTimeoutApp = GuiceApplicationBuilder()
        .configure("metrics.enabled" -> "false", "play.ws.timeout.request" -> "200 millis")
        .build()

      try {
        val timeoutConnector =
          new UpscanInitiateConnector(shortTimeoutApp.injector.instanceOf[HttpClientV2], mockAppConfig)

        wireMockServer.stubFor(
          post(urlPathEqualTo(initiateUrl)).willReturn(aResponse().withStatus(OK).withFixedDelay(2000))
        )

        withCaptureOfLoggingFrom(connectorLogger) { logs =>
          val failure = intercept[ConnectorFailure](
            await(timeoutConnector.initiateUpscan(userId, Some("successRedirectUrl"), Some("errorRedirectUrl")))
          )

          failure.copy(cause = None) shouldBe ConnectorFailure(FailureReason.TIMEOUT)

          logLines(logs) shouldBe List(
            callingLine,
            Level.ERROR -> s"$tag Request timed out outcome=FAILURE reason=TIMEOUT userId=$userId cause=GatewayTimeoutException"
          )
        }
      } finally shortTimeoutApp.stop()
    }

    "fail with CONNECTION_FAILURE and log at error when upscan can't be reached" in {
      // nothing listens on port 1 - the connection is refused
      val unreachableConfig = mock[ApplicationConfig]
      when(unreachableConfig.initiateUrl).thenReturn("http://localhost:1/upscan/v2/initiate")
      when(unreachableConfig.upscanCallbackEndpoint).thenReturn("/ras-api/file-processing/status")
      when(unreachableConfig.rasApiBaseUrl).thenReturn("http://localhost:1")

      val unreachableConnector = new UpscanInitiateConnector(httpClient, unreachableConfig)

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](
          await(unreachableConnector.initiateUpscan(userId, Some("successRedirectUrl"), Some("errorRedirectUrl")))
        )

        failure.copy(cause = None) shouldBe ConnectorFailure(FailureReason.CONNECTION_FAILURE)

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> s"$tag Could not connect outcome=FAILURE reason=CONNECTION_FAILURE userId=$userId cause=BadGatewayException"
        )
      }
    }
  }

}
