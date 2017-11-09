/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class FileUploadFrontendConnectorSpec extends UnitSpec with OneAppPerSuite with MockitoSugar with ServicesConfig {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestConnector extends FileUploadFrontendConnector {
    override val httpPost: HttpPost = mock[HttpPost]
  }

  "File upload frontend connector" when {

    "calling upload frontend service upload endpoint" should {

      "return service response to caller" in {
        val response = HttpResponse(200, None, Map(), None)
        when(TestConnector.httpPost.POST[JsValue, HttpResponse](any(),any(),any())(any(),any(),any(),any())).thenReturn(Future.successful(response))
        val result = await(TestConnector.uploadFile("","",""))
        result shouldBe response
      }

    }

  }

}
