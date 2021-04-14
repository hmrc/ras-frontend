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

package views

import play.api.i18n.Messages
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

class FileUploadSuccessfulViewSpec extends UnitSpec with RasTestHelper {

	"file upload success page" should {
		"display file upload successful page" when {
			"file has been uploaded successfully" in {
				val result = fileUploadSuccessfulView()(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("page-header").text shouldBe Messages("upload.success.header")
			}
		}

		"successful upload page" should {
			"contain the correct content" in {
				val result = fileUploadSuccessfulView()(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("page-header").text shouldBe Messages("upload.success.header")
				doc(result).getElementById("first-description").text shouldBe Messages("upload.success.first-description")
				doc(result).getElementById("second-description").text shouldBe Messages("upload.success.second-description")
				doc(result).getElementById("continue").text shouldBe Messages("continue")
			}

			"contains the correct ga events" in {
				val result = fileUploadSuccessfulView()(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:Your file has been uploaded:Continue"
			}
		}
	}
}
