/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, InternalServerException, UpstreamErrorResponse}
import utils.RasTestHelper

import java.io.InputStream
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.io.Source

class ResidencyStatusAPIConnectorSpec extends AnyWordSpec with Matchers with RasTestHelper {
  val httpClient: HttpClientV2 = fakeApplication.injector.instanceOf[HttpClientV2]
  val connector: ResidencyStatusAPIConnector = new ResidencyStatusAPIConnector(httpClient, mockAppConfig)

  val memberName: MemberName = MemberName("John", "Smith")
  val rasDate: RasDate = RasDate(Some("1"), Some("2"), Some("1990"))
  val memberDetails = MemberDetails(memberName, "AB123456C", rasDate)

  val residencyStatusJson =
    """
      |{
      |  "currentYearResidencyStatus": "scotResident",
      |  "nextYearForecastResidencyStatus": "otherUKResident"
      |}
  """.stripMargin
  val expected = ResidencyStatus("scotResident", Some("otherUKResident"))

  "getResidencyStatus" should {

    "return the success response when API returns 200" in {
      setupMockPost(OK, residencyStatusJson)
      val result: ResidencyStatus = await(connector.getResidencyStatus(memberDetails))
      result shouldBe expected
    }

    "throw internal server error when API returns 400" in {
      setupMockPost(BAD_REQUEST, residencyStatusJson)
      val result = intercept[InternalServerException] {
        await(connector.getResidencyStatus(memberDetails))
      }
      result.getMessage should include("Internal Server Error")
    }

    "throw UpstreamErrorResponse when API returns 403" in {
      setupMockPost(FORBIDDEN, residencyStatusJson)
      val result = intercept[UpstreamErrorResponse] {
        await(connector.getResidencyStatus(memberDetails))
      }
      result.getMessage should include("Member not found")
    }

    "throw InternalServerException when API returns 5XX" in {
      setupMockPost(INTERNAL_SERVER_ERROR, residencyStatusJson)
      val result = intercept[InternalServerException] {
        await(connector.getResidencyStatus(memberDetails))
      }
      result.getMessage should include("Internal Server Error")
    }
  }

  "getFile" should {
    val row = "John,Smith,AB123456C,1990-02-21"

    "return the success response when API returns 200" in {
      setupMockGet(OK, row)
      val result: Option[InputStream] = await(connector.getFile("testFile.csv","A123456"))
      result.isDefined shouldBe true

      val content = Source.fromInputStream(result.get).mkString
      content should include ("John")
    }

    "return empty InputStream when response body is empty" in {
      setupMockGet(OK, "")
      val result: Option[InputStream] = await(connector.getFile("testFile.csv","A123456"))
      result.isDefined shouldBe true

      val content = Source.fromInputStream(result.get).mkString
      content shouldBe empty
    }

    "return InputStream even when response status is 500" in {
      setupMockGet(INTERNAL_SERVER_ERROR, "Internal server error")
      val result: Option[InputStream] = await(connector.getFile("testFile.csv","A123456"))
      result.isDefined shouldBe true

      val content = Source.fromInputStream(result.get).mkString
      content shouldBe "Internal server error"
    }

}

  "deleteFile" should {

    "return the response as OK once the file is deleted successfully" in {
      setupMockDelete(OK)
      val result: HttpResponse = await(connector.deleteFile("testFile.csv", "A123456"))
      result.status shouldBe OK
    }

    "return the response as Bad Request if API returns 400" in {
      setupMockDelete(BAD_REQUEST)
      val result: HttpResponse = await(connector.deleteFile("testFile.csv","A123456"))
      result.status shouldBe BAD_REQUEST
    }
  }

  private def setupMockPost(statusCode: Int, body: String): StubMapping =
    wireMockServer.stubFor(
      post(urlPathEqualTo("/residency-status"))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
            .withBody(body)

        )
    )

  private def setupMockGet(statusCode: Int, body: String): StubMapping = {
    wireMockServer.stubFor(
      get(urlPathEqualTo("/ras-api/file/getFile/testFile.csv"))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
            .withBody(body)

        )
    )
  }

  private def setupMockDelete(statusCode: Int): StubMapping =
    wireMockServer.stubFor(
      delete(urlPathEqualTo("/ras-api/file/remove/testFile.csv/A123456"))
        .willReturn(
          aResponse()
            .withStatus(statusCode)
        )
    )
}
