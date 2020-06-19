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


class NoResultsAvailableViewSpec extends UnitSpec with RasTestHelper {

	"renderNoResultsAvailablePage" should {
		"contain the correct page title" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("no.results.available.page.title")
		}

		"contain the correct page header" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("header").text shouldBe Messages("no.results.available.page.header")
		}

		"contain the correct page content" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("sub-header").text shouldBe Messages("no.results.available.sub-header", Messages("no.results.available.link"))
		}

		"contain a back link pointing to /" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("href") should include("/")
		}

		"contain a choose something else to do button" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
		}

		"contain a choose something else to do button that points to /" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else").attr("href") should include ("/")
		}

		"contain a link back to the upload a file page" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("upload-link").attr("href") should include("/upload-a-file")
		}

		"contain the correct ga events" in {
			val result = views.html.no_results_available()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else").attr("data-journey-click") shouldBe "button - click:You have not uploaded a file:Choose something else to do"
			doc(result).getElementsByClass("link-back").attr("data-journey-click") shouldBe "navigation - link:You have not uploaded a file:Back"
			doc(result).getElementById("upload-link").attr("data-journey-click") shouldBe "link - click:You have not uploaded a file:Upload a file"
		}
	}
}
