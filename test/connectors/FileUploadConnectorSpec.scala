/*
 * Copyright 2020 HM Revenue & Customs
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

import models.{ApiV1_0, ApiV2_0, ApiVersion}
import org.mockito.ArgumentMatcher
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class FileUploadConnectorSpec extends UnitSpec with OneAppPerSuite with MockitoSugar with ServicesConfig {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockHttp = mock[HttpPost]

  def testConnector(version: ApiVersion) = new FileUploadConnector {
    override val http: HttpPost = mockHttp
    override val rasFileUploadCallbackUrl: String = "fake-url"
    override val apiVersion: ApiVersion = version
  }

  def bodyMatcher(version: ApiVersion) = new ArgumentMatcher[JsValue] {
    override def matches(other: scala.Any): Boolean = {
      val residencyStatusVersion = version match {
        case ApiV1_0 => "1.0"
        case ApiV2_0 => "2.0"
      }
      other match {
        case body: JsValue => (body \ "callbackUrl").validate[String].get.matches(s"^.*/?version=$residencyStatusVersion$$")
        case _ => false
      }
    }
  }

  "File upload connector" when {
    Seq(ApiV1_0, ApiV2_0) foreach { testApiVersion =>

      s"api version is $testApiVersion" when {
        "calling file upload service create envelope endpoint" should {
          "return service response to caller" in {
            val response = HttpResponse(201, None, Map("Location" -> List("localhost:8898/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653")), None)
            when(mockHttp.POST[JsValue, HttpResponse](any(), argThat(bodyMatcher(testApiVersion)), any())(any(), any(), any(), any())).thenReturn(Future.successful(response))
            val result = await(testConnector(testApiVersion).createEnvelope(""))
            result shouldBe response
          }
        }
      }

    }
  }

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
