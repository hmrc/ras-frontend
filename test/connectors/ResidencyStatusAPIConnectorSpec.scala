/*
 * Copyright 2019 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.WSHttp
import models._
import org.mockito.ArgumentMatcher
import org.mockito.Matchers.{eq => Meq, _}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.libs.ws.{DefaultWSResponseHeaders, StreamedResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ResidencyStatusAPIConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar with ServicesConfig {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val mockWsHttp = mock[WSHttp]

  def createTestConnector(apiVersion: ApiVersion) = new ResidencyStatusAPIConnector {
    override val http: WSHttp = mockWsHttp
    override lazy val residencyStatusVersion = apiVersion
  }

  val testConnector: ResidencyStatusAPIConnector = createTestConnector(apiVersion = ApiV2_0)

  def headerCarrierMatcher(version: ApiVersion) = new ArgumentMatcher[HeaderCarrier] {
    override def matches(other: scala.Any): Boolean = {
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

          when(mockWsHttp.POST[MemberDetails, ResidencyStatus](any(), any(), any())(
            any(), any(), argThat(headerCarrierMatcher(testApiVersion)), any())
          ).thenReturn(Future.successful(expectedResponse))

          val result = testConnector.getResidencyStatus(memberDetails)

          await(result) shouldBe expectedResponse
        }
      }

      "the date is between 1st January and 5th April" in {

        val memberDetails = MemberDetails(MemberName("John", "Smith"), "AB123456C", RasDate(Some("21"), Some("09"), Some("1970")))

        val expectedResponse = ResidencyStatus("scotResident", Some("otherUKResident"))

        when(mockWsHttp.POST[MemberDetails, ResidencyStatus](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(expectedResponse))

        val result = testConnector.getResidencyStatus(memberDetails)

        await(result) shouldBe expectedResponse

      }

      "the date is between 6th April and 31st December" in {

        val memberDetails = MemberDetails(MemberName("John", "Smith"), "AB123456C", RasDate(Some("21"), Some("09"), Some("1970")))

        val expectedResponse = ResidencyStatus("scotResident", None)

        when(mockWsHttp.POST[MemberDetails, ResidencyStatus](any(), any(), any())(any(), any(), any(), any())).thenReturn(Future.successful(expectedResponse))

        val result = testConnector.getResidencyStatus(memberDetails)

        await(result) shouldBe expectedResponse
      }
    }
  }

  "getFile" should {

    "return an StreamedResponse from ras-api service" in {

      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()

      val streamResponse: StreamedResponse = StreamedResponse(DefaultWSResponseHeaders(200, Map("CONTENT_TYPE" -> Seq("application/octet-stream"))),
        Source.apply[ByteString](Seq(ByteString("Test"), ByteString("\r\n"), ByteString("Passed")).to[scala.collection.immutable.Iterable]))

      when(mockWsHttp.buildRequestWithStream(any())(any())).thenReturn(Future.successful(streamResponse))

      val values = List("Test", "Passed")

      val result = await(testConnector.getFile("file1", "A1234567"))

      val reader = new BufferedReader(new InputStreamReader(result.get))

      (Iterator continually reader.readLine takeWhile (_ != null) toList) should contain theSameElementsAs List("Test", "Passed")

    }
  }

  "deleteFile" should {
    "return a 200 when a file has been successfully deleted" in {

      when(mockWsHttp.DELETE[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val result = testConnector.deleteFile("file-name", "userId")

      await(result).status shouldBe 200
    }

    "return a 500 when a file has not been deleted" in {
      when(mockWsHttp.DELETE[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(500)))

      val result = testConnector.deleteFile("file-name", "userId")

      await(result).status shouldBe 500
    }
  }

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
