/*
 * Copyright 2021 HM Revenue & Customs
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

import java.io.{BufferedReader, InputStreamReader}

import akka.stream.scaladsl.Source
import akka.util.ByteString
import models._
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.libs.ws.ahc.AhcWSResponse
import play.api.libs.ws.{StandaloneWSResponse, WSRequest, WSResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ResidencyStatusAPIConnectorSpec extends UnitSpec with utils.RasTestHelper {

  def createTestConnector(apiVersion: ApiVersion): ResidencyStatusAPIConnector = new ResidencyStatusAPIConnector(mockHttp, mockAppConfig) {
    override lazy val residencyStatusVersion: ApiVersion = apiVersion
  }

  val testConnector: ResidencyStatusAPIConnector = createTestConnector(apiVersion = ApiV2_0)

  def headerCarrierMatcher(version: ApiVersion): ArgumentMatcher[HeaderCarrier] = new ArgumentMatcher[HeaderCarrier] {
    override def matches(other: HeaderCarrier): Boolean = {
      val residencyStatusVersion = version match {
        case ApiV1_0 => "1.0"
        case ApiV2_0 => "2.0"
      }
      val expectedHeaders = Set(
        "Accept" -> s"application/vnd.hmrc.$residencyStatusVersion+json",
        "Content-Type" -> "application/json"
      )
      other match {
        case hc: HeaderCarrier => expectedHeaders.forall(hc.headers.contains)
        case _ => false
      }
    }
  }

  "Residency Status API connector" should {
    "send a get request to residency status service" when {
      Seq(ApiV1_0, ApiV2_0) foreach { testApiVersion =>
        s"api version is $testApiVersion" in {
          val memberDetails = MemberDetails(MemberName("John", "Smith"), "AB123456C", RasDate(Some("21"), Some("09"), Some("1970")))
					val expectedResponse = ResidencyStatus("scotResident", Some("otherUKResident"))
          val testConnector = createTestConnector(apiVersion = testApiVersion)
          when(mockHttp.POST[MemberDetails, ResidencyStatus](any(), any(), any())(
            any(), any(), argThat(headerCarrierMatcher(testApiVersion)), any())
          ).thenReturn(Future.successful(expectedResponse))

          val result = testConnector.getResidencyStatus(memberDetails)
          await(result) shouldBe expectedResponse
        }
      }

      "the date is between 1st January and 5th April" in {
        val memberDetails = MemberDetails(MemberName("John", "Smith"), "AB123456C", RasDate(Some("21"), Some("09"), Some("1970")))
        val expectedResponse = ResidencyStatus("scotResident", Some("otherUKResident"))
        when(mockHttp.POST[MemberDetails, ResidencyStatus](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(expectedResponse))
				val result = testConnector.getResidencyStatus(memberDetails)
        await(result) shouldBe expectedResponse
      }

      "the date is between 6th April and 31st December" in {
        val memberDetails = MemberDetails(MemberName("John", "Smith"), "AB123456C", RasDate(Some("21"), Some("09"), Some("1970")))
        val expectedResponse = ResidencyStatus("scotResident", None)
        when(mockHttp.POST[MemberDetails, ResidencyStatus](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(expectedResponse))
        val result = testConnector.getResidencyStatus(memberDetails)
        await(result) shouldBe expectedResponse
      }
    }
  }

  "getFile" should {
    "return an StreamedResponse from ras-api service" in {

			val mockR = mock[StandaloneWSResponse]
			val mockWSRequest = mock[WSRequest]
			lazy val bodyAsSourceResponse: Source[ByteString, _] = Source.apply[ByteString](Seq(ByteString("Test"), ByteString("\n"), ByteString("Passed")).to[scala.collection.immutable.Iterable])

      when(mockHttp.buildRequest(any(), any())(any())).thenReturn(Future.successful(mockWSRequest))
			when(mockWSRequest.stream()).thenReturn(Future.successful(AhcWSResponse(mockR)))
			when(mockR.bodyAsSource).thenAnswer(
				new Answer[Source[ByteString, _]] {
					override def answer(invocation: InvocationOnMock): Source[ByteString, _] = bodyAsSourceResponse
				}
			)

			val result = await(testConnector.getFile("file1", "A1234567"))
      val reader = new BufferedReader(new InputStreamReader(result.get))
      (Iterator continually reader.readLine takeWhile (_ != null) toList) should contain theSameElementsAs List("Test", "Passed")

    }
  }

  "deleteFile" should {
    "return a 200 when a file has been successfully deleted" in {
      when(mockHttp.DELETE[HttpResponse](any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200)))
      val result = testConnector.deleteFile("file-name", "userId")
      await(result).status shouldBe 200
    }

    "return a 500 when a file has not been deleted" in {
      when(mockHttp.DELETE[HttpResponse](any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(500)))
      val result = testConnector.deleteFile("file-name", "userId")
      await(result).status shouldBe 500
    }
  }
}
