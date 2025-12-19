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

import models._
import models.upscan.UpscanInitiateResponse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import utils.RasTestHelper

class UpscanInitiateConnectorSpec extends AnyWordSpec with Matchers with RasTestHelper {
  val httpClient: HttpClientV2 = fakeApplication.injector.instanceOf[HttpClientV2]


  val connector: UpscanInitiateConnector = new UpscanInitiateConnector(httpClient, mockAppConfig)
  val upscanInitiateResponseJson : String =
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

  "upscan initiate" should {

    "returns valid successful response" in {
      setupMockPost(OK, upscanInitiateResponseJson, "/upscan/v2/initiate")
      val result: UpscanInitiateResponse = await(connector.initiateUpscan("A123456", Some("successRedirectUrl"),  Some("errorRedirectUrl")))

      result.fileReference.reference shouldBe "reference-1234"
      result.postTarget shouldBe "downloadUrl"
    }

    "throw an exception" when {

      "upscan returns a 4xx response" in {
        setupMockPost(BAD_REQUEST, "{}", "/upscan/v2/initiate")
        val exception = intercept[UpstreamErrorResponse] {
          await(connector.initiateUpscan("A123456", Some("successRedirectUrl"),  Some("errorRedirectUrl")))
        }
        exception.statusCode shouldBe 400
      }

      "upscan returns 5xx response" in {
        setupMockPost(SERVICE_UNAVAILABLE, "{}", "/upscan/v2/initiate")
        val exception = intercept[UpstreamErrorResponse] {
          await(connector.initiateUpscan("A123456", Some("successRedirectUrl"),  Some("errorRedirectUrl")))
        }
        exception.statusCode shouldBe 503
      }
    }
  }

}
