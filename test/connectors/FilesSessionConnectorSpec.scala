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
import models.{CallbackData, CreateFileSessionRequest, FileSession, ResultsFileMetaData}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Logger
import play.api.http.Status.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing
import utils.RasTestHelper

class FilesSessionConnectorSpec extends AnyWordSpec with Matchers with RasTestHelper with LogCapturing {

  val httpClient: HttpClientV2         = fakeApplication.injector.instanceOf[HttpClientV2]
  val connector: FilesSessionConnector = new FilesSessionConnector(httpClient, mockAppConfig)

  private val connectorLogger = Logger(classOf[FilesSessionConnector])

  private val createTag = "[FilesSessionConnector][createFileSession]"
  private val fetchTag  = "[FilesSessionConnector][fetchFileSession]"
  private val deleteTag = "[FilesSessionConnector][deleteFileSession]"

  val userId    = "A123456"
  val reference = "reference-1234"

  val callbackData: CallbackData       = CallbackData(reference, None, "READY", None, None)
  val resultsFile: ResultsFileMetaData = ResultsFileMetaData("file-id-1", Some("fileName.csv"), Some(1234L), 123, 1234L)

  val fileSession: Option[FileSession] = Some(
    FileSession(Some(callbackData), Some(resultsFile), userId, Some(1765890906194L), None)
  )

  val fileSessionJson: String =
    """
      |{
      |  "userFile": {
      |    "reference": "reference-1234",
      |    "fileStatus": "READY"
      |  },
      |  "resultsFile": {
      |    "id": "file-id-1",
      |    "filename": "fileName.csv",
      |    "uploadDate": 1234,
      |    "chunkSize": 123,
      |    "length": 1234
      |  },
      |  "userId": "A123456",
      |  "uploadTimeStamp": 1765890906194
      |}
    """.stripMargin

  private def logLines(logs: List[ILoggingEvent]): List[(Level, String)] =
    logs.map(event => event.getLevel -> event.getFormattedMessage)

  "fetchFileSession" should {

    "return the file session, without logging, when the API returns 200" in {
      setupMockGet(OK, fileSessionJson, s"/get-file-session/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Option[FileSession] = await(connector.fetchFileSession(userId))

        result shouldBe fileSession
        wireMockServer.verify(getRequestedFor(urlEqualTo(s"/get-file-session/$userId")))

        logLines(logs) shouldBe empty
      }
    }

    "return None and log at info when the API returns 404 (no file session)" in {
      setupMockGet(NOT_FOUND, "", s"/get-file-session/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Option[FileSession] = await(connector.fetchFileSession(userId))

        result shouldBe None

        logLines(logs) shouldBe List(
          Level.INFO -> s"$fetchTag No file session found userId=$userId status=404"
        )
      }
    }

    "return None and log at warn when the API returns any other non-OK status" in {
      setupMockGet(BAD_REQUEST, "{}", s"/get-file-session/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Option[FileSession] = await(connector.fetchFileSession(userId))

        result shouldBe None
        wireMockServer.verify(getRequestedFor(urlEqualTo(s"/get-file-session/$userId")))

        logLines(logs) shouldBe List(
          Level.WARN -> (s"$fetchTag Received non-OK status code from API " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=400")
        )
      }
    }

    "return None and log INVALID_RESPONSE with the JSON paths when a 200 body is JSON in the wrong shape" in {
      setupMockGet(OK, """{"test":"value"}""", s"/get-file-session/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Option[FileSession] = await(connector.fetchFileSession(userId))

        result shouldBe None

        logLines(logs) shouldBe List(
          Level.ERROR -> (s"$fetchTag File session json did not match expected format " +
            s"outcome=FAILURE reason=INVALID_RESPONSE userId=$userId status=200 parseErrors=/userId: error.path.missing")
        )
      }
    }

    "return None and log PARSE_ERROR, without logging the body, when a 200 body is not JSON" in {
      // the parser's message would quote this value, so check it's not logged below
      setupMockGet(OK, """{"userId": A123456}""", s"/get-file-session/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        await(connector.fetchFileSession(userId)) shouldBe None

        logLines(logs) shouldBe List(
          Level.ERROR -> (s"$fetchTag File session response was not valid json " +
            s"outcome=FAILURE reason=PARSE_ERROR userId=$userId status=200 cause=JsonParseException")
        )
      }
    }
  }

  "createFileSession" should {
    val request: CreateFileSessionRequest = CreateFileSessionRequest(userId, reference)

    "return true and log success at info when the API returns 201" in {
      setupMockPost(CREATED, "", "/create-file-session")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Boolean = await(connector.createFileSession(request))

        result shouldBe true
        wireMockServer.verify(postRequestedFor(urlEqualTo("/create-file-session")))

        logLines(logs) shouldBe List(
          Level.INFO -> s"$createTag outcome=SUCCESS userId=$userId status=201 reference=$reference"
        )
      }
    }

    "return false and log at error when the API returns 400" in {
      setupMockPost(BAD_REQUEST, "{}", "/create-file-session")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Boolean = await(connector.createFileSession(request))

        result shouldBe false
        wireMockServer.verify(postRequestedFor(urlEqualTo("/create-file-session")))

        logLines(logs) shouldBe List(
          Level.ERROR -> (s"$createTag File session was not created reference=$reference " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=400")
        )
      }
    }

    "return false and log the actual status when the API returns 500" in {
      setupMockPost(INTERNAL_SERVER_ERROR, "", "/create-file-session")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        await(connector.createFileSession(request)) shouldBe false

        logLines(logs) shouldBe List(
          Level.ERROR -> (s"$createTag File session was not created reference=$reference " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=500")
        )
      }
    }
  }

  "deleteFileSession" should {

    "return true and log success at info when the API returns 204" in {
      setupMockDelete(NO_CONTENT, "", s"/delete-file-session/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Boolean = await(connector.deleteFileSession(userId))

        result shouldBe true
        wireMockServer.verify(deleteRequestedFor(urlEqualTo(s"/delete-file-session/$userId")))

        logLines(logs) shouldBe List(
          Level.INFO -> s"$deleteTag outcome=SUCCESS userId=$userId status=204"
        )
      }
    }

    "return false and log at warn when the deletion fails" in {
      setupMockDelete(BAD_REQUEST, "{}", s"/delete-file-session/$userId")

      withCaptureOfLoggingFrom(connectorLogger) { logs =>
        val result: Boolean = await(connector.deleteFileSession(userId))

        result shouldBe false
        wireMockServer.verify(deleteRequestedFor(urlEqualTo(s"/delete-file-session/$userId")))

        logLines(logs) shouldBe List(
          Level.WARN -> (s"$deleteTag File session was not deleted " +
            s"outcome=FAILURE reason=INTERNAL_SERVER_ERROR userId=$userId status=400")
        )
      }
    }
  }

}
