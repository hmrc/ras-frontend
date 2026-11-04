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
import com.github.tomakehurst.wiremock.client.WireMock.*
import config.ApplicationConfig
import models.*
import org.apache.pekko.stream.Materializer
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Logger
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, RequestId}
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import utils.RasTestHelper

import java.io.InputStream
import scala.io.Source

class ResidencyStatusAPIConnectorSpec extends AnyWordSpec with Matchers with RasTestHelper with LogCapturing {

  val httpClient: HttpClientV2               = fakeApplication.injector.instanceOf[HttpClientV2]
  val mat: Materializer                      = fakeApplication.injector.instanceOf[Materializer]
  val connector: ResidencyStatusAPIConnector = new ResidencyStatusAPIConnector(httpClient, mockAppConfig, mat)

  private val connectorLogger = Logger(classOf[ResidencyStatusAPIConnector])

  val userId                 = "A123456"
  val nino                   = "AB123456C"
  val memberName: MemberName = MemberName("John", "Smith")
  val rasDate: RasDate       = RasDate(Some("1"), Some("2"), Some("1990"))
  val memberDetails          = MemberDetails(memberName, nino, rasDate)
  val fileName               = "testFile.csv"

  val residencyStatusJson: String =
    """
      |{
      |  "currentYearResidencyStatus": "scotResident",
      |  "nextYearForecastResidencyStatus": "otherUKResident"
      |}
    """.stripMargin

  val expectedResponse = ResidencyStatus("scotResident", Some("otherUKResident"))

  private val lookupTag     = "[ResidencyStatusAPIConnector][getResidencyStatus]"
  private val getFileTag    = "[ResidencyStatusAPIConnector][getFile]"
  private val deleteFileTag = "[ResidencyStatusAPIConnector][deleteFile]"

  private val callingLine = Level.INFO -> s"$lookupTag Calling Residency Status api"

  private def logLines(logs: List[ILoggingEvent]): List[(Level, String)] =
    logs.map(event => event.getLevel -> event.getFormattedMessage)

  "getResidencyStatus" should {

    "return the residency status and log the call and success at info when the API returns 200" in {
      setupMockPost(OK, residencyStatusJson, "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        await(connector.getResidencyStatus(memberDetails, userId)) shouldBe expectedResponse

        logLines(logs) shouldBe List(
          callingLine,
          Level.INFO -> s"$lookupTag outcome=SUCCESS userId=$userId status=200"
        )
      }
    }

    "fail with MATCHING_FAILED and log at info when the API returns 403" in {
      setupMockPost(FORBIDDEN, "{}", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        failure shouldBe ConnectorFailure(FailureReason.MATCHING_FAILED, Some(FORBIDDEN))

        logLines(logs) shouldBe List(
          callingLine,
          Level.INFO -> s"$lookupTag Member not found outcome=FAILURE reason=MATCHING_FAILED userId=$userId status=403"
        )
      }
    }

    "fail with BAD_REQUEST and log at error when the API returns 400" in {
      setupMockPost(BAD_REQUEST, "{}", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        failure shouldBe ConnectorFailure(FailureReason.BAD_REQUEST, Some(BAD_REQUEST))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$lookupTag Data sent to the API was not in the correct format " +
            s"outcome=FAILURE reason=BAD_REQUEST userId=$userId status=400")
        )
      }
    }

    "fail with TOO_MANY_REQUESTS and log at warn when the API returns 429" in {
      setupMockPost(TOO_MANY_REQUESTS, "{}", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        failure shouldBe ConnectorFailure(FailureReason.TOO_MANY_REQUESTS, Some(TOO_MANY_REQUESTS))

        logLines(logs) shouldBe List(
          callingLine,
          Level.WARN -> s"$lookupTag outcome=FAILURE reason=TOO_MANY_REQUESTS userId=$userId status=429"
        )
      }
    }

    "fail with SERVICE_UNAVAILABLE and log at warn when the API returns 503" in {
      setupMockPost(SERVICE_UNAVAILABLE, "{}", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        failure shouldBe ConnectorFailure(FailureReason.SERVICE_UNAVAILABLE, Some(SERVICE_UNAVAILABLE))

        logLines(logs) shouldBe List(
          callingLine,
          Level.WARN -> s"$lookupTag outcome=FAILURE reason=SERVICE_UNAVAILABLE userId=$userId status=503"
        )
      }
    }

    "fail with INTERNAL_SERVER_ERROR and log the actual status at error when the API returns 500" in {
      setupMockPost(INTERNAL_SERVER_ERROR, "{}", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        failure shouldBe ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, Some(INTERNAL_SERVER_ERROR))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$lookupTag Unexpected status code received from RAS-API " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=500")
        )
      }
    }

    "keep the actual status for other unexpected statuses, e.g. 401" in {
      setupMockPost(UNAUTHORIZED, "{}", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        failure shouldBe ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, Some(UNAUTHORIZED))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$lookupTag Unexpected status code received from RAS-API " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=401")
        )
      }
    }

    "fail with INVALID_RESPONSE, logging the JSON paths, when a 200 body is JSON in the wrong shape" in {
      setupMockPost(OK, """{"unexpected": "value"}""", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        val parseErrors = "/currentYearResidencyStatus: error.path.missing"

        failure shouldBe ConnectorFailure(FailureReason.INVALID_RESPONSE, Some(OK), parseErrors = Some(parseErrors))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$lookupTag Response json did not match expected format " +
            s"outcome=FAILURE reason=INVALID_RESPONSE userId=$userId status=200 parseErrors=$parseErrors")
        )
      }
    }

    "fail with PARSE_ERROR, without logging the body, when a 200 body is not JSON" in {
      // the parser's message would quote this value, so check it's not logged below
      setupMockPost(OK, """{"nino": AB123456C}""", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)))

        failure.copy(cause = None) shouldBe ConnectorFailure(FailureReason.PARSE_ERROR, Some(OK))

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> (s"$lookupTag Response body was not valid json " +
            s"outcome=FAILURE reason=PARSE_ERROR userId=$userId status=200 cause=JsonParseException")
        )
      }
    }
  }

  "getResidencyStatus transport failures" should {

    "fail with TIMEOUT and log at error when the API doesn't respond in time" in {
      val shortTimeoutApp = GuiceApplicationBuilder()
        .configure("metrics.enabled" -> "false", "play.ws.timeout.request" -> "200 millis")
        .build()

      try {
        val timeoutConnector =
          new ResidencyStatusAPIConnector(shortTimeoutApp.injector.instanceOf[HttpClientV2], mockAppConfig, mat)

        wireMockServer.stubFor(
          post(urlPathEqualTo("/residency-status")).willReturn(aResponse().withStatus(OK).withFixedDelay(2000))
        )

        withCaptureOfLoggingFrom(connectorLogger) { logs =>
          val failure = intercept[ConnectorFailure](await(timeoutConnector.getResidencyStatus(memberDetails, userId)))

          failure.copy(cause = None)                  shouldBe ConnectorFailure(FailureReason.TIMEOUT)
          failure.cause.map(_.getClass.getSimpleName) shouldBe Some("GatewayTimeoutException")

          logLines(logs) shouldBe List(
            callingLine,
            Level.ERROR -> s"$lookupTag Request timed out outcome=FAILURE reason=TIMEOUT userId=$userId cause=GatewayTimeoutException"
          )
        }
      } finally shortTimeoutApp.stop()
    }

    "fail with CONNECTION_FAILURE and log at error when the API can't be reached" in {
      // nothing listens on port 1 - the connection is refused
      val unreachableConfig = mock[ApplicationConfig]
      when(unreachableConfig.rasApiBaseUrl).thenReturn("http://localhost:1")
      when(unreachableConfig.rasApiResidencyStatusEndpoint).thenReturn("residency-status")

      val unreachableConnector = new ResidencyStatusAPIConnector(httpClient, unreachableConfig, mat)

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(unreachableConnector.getResidencyStatus(memberDetails, userId)))

        failure.copy(cause = None)                  shouldBe ConnectorFailure(FailureReason.CONNECTION_FAILURE)
        failure.cause.map(_.getClass.getSimpleName) shouldBe Some("BadGatewayException")

        logLines(logs) shouldBe List(
          callingLine,
          Level.ERROR -> s"$lookupTag Could not connect outcome=FAILURE reason=CONNECTION_FAILURE userId=$userId cause=BadGatewayException"
        )
      }
    }
  }

  "getFile" should {
    val record        = "John,Smith,AB123456C,1990-02-21"
    val getFileCalled = Level.INFO -> s"$getFileTag Get results file with URI for $fileName by userId ($userId)"

    "return the file and log success at info when the API returns 200" in {
      setupMockGet(OK, record, s"/ras-api/file/getFile/$fileName")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Option[InputStream] = await(connector.getFile(fileName, userId))

        val value   = result.get
        val content = Source.fromInputStream(value).mkString
        value.close()
        content shouldBe record

        logLines(logs) shouldBe List(
          getFileCalled,
          Level.INFO -> s"$getFileTag outcome=SUCCESS userId=$userId status=200 fileName=$fileName"
        )
      }
    }

    "return an empty InputStream when the response body is empty" in {
      setupMockGet(OK, "", s"/ras-api/file/getFile/$fileName")

      val result: Option[InputStream] = await(connector.getFile(fileName, userId))

      Source.fromInputStream(result.get).mkString shouldBe empty
    }

    "fail, rather than returning the error body as the file, and log at error when the API returns 500" in {
      setupMockGet(INTERNAL_SERVER_ERROR, "Internal server error", s"/ras-api/file/getFile/$fileName")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure = intercept[ConnectorFailure](await(connector.getFile(fileName, userId)))

        failure shouldBe ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, Some(INTERNAL_SERVER_ERROR))

        logLines(logs) shouldBe List(
          getFileCalled,
          Level.ERROR -> (s"$getFileTag Results file could not be retrieved fileName=$fileName " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=500")
        )
      }
    }

    "fail with the actual status when the API returns 404" in {
      setupMockGet(NOT_FOUND, "", s"/ras-api/file/getFile/$fileName")

      val failure = intercept[ConnectorFailure](await(connector.getFile(fileName, userId)))

      failure shouldBe ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, Some(NOT_FOUND))
    }
  }

  "deleteFile" should {

    "return the response and log success at info once the file is deleted" in {
      setupMockDelete(OK, "", s"/ras-api/file/remove/$fileName/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: HttpResponse = await(connector.deleteFile(fileName, userId))

        result.status shouldBe OK

        logLines(logs) shouldBe List(
          Level.INFO -> s"$deleteFileTag outcome=SUCCESS userId=$userId status=200 fileName=$fileName"
        )
      }
    }

    "still return the response, but log at warn, when the API returns 400" in {
      setupMockDelete(BAD_REQUEST, "", s"/ras-api/file/remove/$fileName/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: HttpResponse = await(connector.deleteFile(fileName, userId))

        result.status shouldBe BAD_REQUEST

        logLines(logs) shouldBe List(
          Level.WARN -> (s"$deleteFileTag Results file was not deleted fileName=$fileName " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=400")
        )
      }
    }
  }

  "request ID propagation" should {
    val requestId       = "govuk-tax-test-request-id"
    val hcWithRequestId = HeaderCarrier(requestId = Some(RequestId(requestId)))

    "send X-Request-ID to RAS-API and include it in failure log lines for getResidencyStatus" in {
      setupMockPost(FORBIDDEN, "{}", "/residency-status")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val failure =
          intercept[ConnectorFailure](await(connector.getResidencyStatus(memberDetails, userId)(hcWithRequestId, ec)))

        failure shouldBe ConnectorFailure(FailureReason.MATCHING_FAILED, Some(FORBIDDEN))

        wireMockServer.verify(
          postRequestedFor(urlEqualTo("/residency-status")).withHeader("X-Request-ID", equalTo(requestId))
        )

        logLines(logs) shouldBe List(
          callingLine,
          Level.INFO -> (s"$lookupTag Member not found outcome=FAILURE reason=MATCHING_FAILED " +
            s"userId=$userId requestId=$requestId status=403")
        )
      }
    }

    "send X-Request-ID to RAS-API for getFile" in {
      setupMockGet(OK, "content", s"/ras-api/file/getFile/$fileName")

      await(connector.getFile(fileName, userId)(hcWithRequestId, ec)).foreach(_.close())

      wireMockServer.verify(
        getRequestedFor(urlEqualTo(s"/ras-api/file/getFile/$fileName")).withHeader("X-Request-ID", equalTo(requestId))
      )
    }
  }

}
