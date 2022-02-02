/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.Matchers.{convertToAnyShouldWrapper, include}
import play.api.i18n.Messages
import org.scalatest.WordSpecLike
import utils.RasTestHelper


class FileReadyViewSpec extends WordSpecLike with RasTestHelper {

	"file ready page" must {
		"contain the correct page title" in {
			val result = fileReadyView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("file.ready.page.title")
		}

		"contain the correct page header" in {
			val result = fileReadyView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementsByClass("govuk-heading-xl").text shouldBe Messages("file.ready.page.header")
		}

		"contain a back link pointing to /" in {
			val result = fileReadyView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("href") should include("/")
		}

		"contains the download-your-file-link" in {
			val result = fileReadyView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("download-your-file-link").text shouldBe Messages("file.ready.sub-header")
		}

		"the sub header contains a link that points to download results page" in {
			val result = fileReadyView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("download-your-file-link").attr("href") should include("/residency-status-added")
		}

		"contain the correct ga events" in {
			val result = fileReadyView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:File ready:Back"
			doc(result).getElementById("download-your-file-link").attr("data-journey-click") shouldBe "link - click:File ready:Download your file"
		}
	}
}
