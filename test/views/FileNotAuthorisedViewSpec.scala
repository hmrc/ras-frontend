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

class FileNotAuthorisedViewSpec extends UnitSpec with RasTestHelper {

	"file not available page" should {

		"contain the correct page title" in {
			val result = views.html.file_not_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("file.not.available.page.title")
		}

		"contain the correct page header" in {
			val result = views.html.file_not_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("header").text shouldBe Messages("file.not.available.page.header")
		}

		"contain a back link pointing to /relief-at-source" in {
			val result = views.html.file_not_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("href") shouldBe "/relief-at-source"
		}

		"contain the correct content paragraph" in {
			val result = views.html.file_not_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("sub-header").text shouldBe Messages("file.not.available.sub-header", Messages("file.not.available.link"))
		}

		"contain the correct link in the content paragraph" in {
			val result = views.html.file_not_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("sub-header-link").attr("href") shouldBe "/relief-at-source"
		}

		"contain the correct ga events" in {
			val result = views.html.file_not_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:File not available:Back"
			doc(result).getElementById("sub-header-link").attr("data-journey-click") shouldBe "link - click:File not available:Choose something else to do"
		}
	}
}
