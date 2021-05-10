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

import org.jsoup.Jsoup
import org.scalatest.Matchers.{convertToAnyShouldWrapper, include}
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, _}
import org.scalatest.WordSpecLike
import utils.RasTestHelper

class ProblemUploadingFileViewSpec extends WordSpecLike with RasTestHelper{

	"problem uploading file page" must {

		"contain correct title and header when problem uploading file" in {
			val result = problemUploadingFileView()(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))
			doc.title shouldBe Messages("problem.uploading.file.title")
			doc.getElementsByClass("govuk-back-link").attr("href") should include("/upload-a-file")
			doc.getElementById("header").text shouldBe Messages("problem.uploading.file.header")
			doc.getElementById("try-again").text shouldBe Messages("upload.file.again").capitalize
			doc.getElementById("check-file").text shouldBe Messages("check.file")
			doc.getElementById("return-to-upload").text shouldBe Messages("return.to.upload")
		}

		"contain correct ga events when problem uploading file" in {
			val result = problemUploadingFileView()(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))
			doc.getElementsByClass("govuk-back-link").attr("data-journey-click") shouldBe "navigation - link:There has been a problem uploading your file:Back"
			doc.getElementById("return-to-upload").attr("data-journey-click") shouldBe "button - click:There has been a problem uploading your file:Return to upload a file"
		}
	}

}
