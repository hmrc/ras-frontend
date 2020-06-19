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

package views

import play.api.i18n.Messages
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

class FileUploadViewSpec extends UnitSpec with RasTestHelper {

	"file upload page" should {
		"contain a back link" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementsByClass("link-back").text shouldBe Messages("back")
		}

		"contain 'upload file' title and header" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).title() shouldBe Messages("file.upload.page.title")
			doc(result).getElementById("header").text shouldBe Messages("file.upload.page.header")
		}

		"contain sub-header" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("sub-header").html shouldBe Messages("file.upload.page.sub-header")
		}

		"contain 'choose file' button" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-file") shouldNot be(null)
		}

		"contain an upload button" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("continue").text shouldBe Messages("continue")
		}

		"contain an uploading help link that opens new window when clicked" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("upload-help-link").text shouldBe Messages("get.help.uploading.link")
			doc(result).getElementById("upload-help-link").attr("target") shouldBe "_blank"
		}

		"contains the correct ga events" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:Upload a file:Continue"
			doc(result).getElementsByClass("link-back").attr("data-journey-click") shouldBe "navigation - link:Upload a file:Back"
			doc(result).getElementById("choose-file").attr("data-journey-click") shouldBe "button - click:Upload a file:Choose file"
			doc(result).getElementById("upload-help-link").attr("data-journey-click") shouldBe "link - click:Upload a file:Get help formatting your file"
		}

		"the get help link should be correct" in {
			val result = views.html.file_upload("fileUploadUrl", "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("upload-help-link").attr("href") shouldBe "http://www.gov.uk/guidance/find-the-relief-at-source-residency-statuses-of-multiple-members"
		}

		"contain empty file error if present in session cache" in {
			val result = views.html.file_upload("fileUploadUrl", "This CSV is empty. Upload a CSV with data in it")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("upload-error").text shouldBe Messages("file.empty.error")
		}
	}
}
