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

package views

import models.upscan.{UpscanFileReference, UpscanInitiateResponse}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers.{be, convertToAnyShouldWrapper}
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import utils.RasTestHelper

class FileUploadViewSpec extends AnyWordSpec with RasTestHelper {

	val upscanResponse: models.upscan.UpscanInitiateResponse = UpscanInitiateResponse(UpscanFileReference(""), "", Map("" -> ""))

	"file upload page" when {
		"contain a back link" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementsByClass("govuk-back-link").text shouldBe Messages("back")
		}

		"contain 'upload file' title and header" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).title() shouldBe Messages("file.upload.page.title")
			doc(result).getElementsByClass("govuk-heading-xl").text shouldBe Messages("file.upload.page.header")
		}

		"contain upload-info" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("one-file-at-a-time").html shouldBe Messages("file.upload.page.point.one")
			doc(result).getElementById("csv-file").html shouldBe Messages("file.upload.page.point.two")
			doc(result).getElementById("smaller-than").html shouldBe Messages("file.upload.page.point.three")
		}

		"contain 'choose file' button" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-file") mustNot be(null)
		}

		"contain an upload button" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("continue").text shouldBe Messages("upload.continue")
		}

		"contain an uploading help link that opens new window when clicked" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("upload-help-link").text shouldBe Messages("get.help.uploading.link")
			doc(result).getElementById("upload-help-link").attr("target") shouldBe "_blank"
		}

		"contains the correct ga events" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:Upload a file:Continue"
			doc(result).getElementsByClass("govuk-back-link").attr("data-journey-click") shouldBe "navigation - link:Upload a file:Back"
			doc(result).getElementById("choose-file").attr("data-journey-click") shouldBe "button - click:Upload a file:Choose file"
			doc(result).getElementById("upload-help-link").attr("data-journey-click") shouldBe "link - click:Upload a file:Get help formatting your file"
		}

		"the get help link should be correct" in {
			val result = fileUploadView(upscanResponse, "errorMessage")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("upload-help-link").attr("href") shouldBe "http://www.gov.uk/guidance/find-the-relief-at-source-residency-statuses-of-multiple-members"
		}

		"contain empty file error if present in session cache" in {
			val result = fileUploadView(upscanResponse, "file.empty.error")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("upload-error").text shouldBe Messages("file.empty.error")
		}
	}
}
