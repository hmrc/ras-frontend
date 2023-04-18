/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, include}
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import utils.RasTestHelper


class ResultsNotAvailableYetViewSpec extends AnyWordSpec with RasTestHelper {

	"results not available yet page" must {
		"contain the correct page title" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("results.not.available.yet.page.title")
		}

		"contain the correct page header" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementsByClass("govuk-heading-xl").text shouldBe Messages("results.not.available.yet.page.header")
		}

		"contain the correct sub header 1" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("sub-header1").text shouldBe Messages("results.not.available.yet.sub-header1")
		}

		"contain the correct sub header 2" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("sub-header2").text shouldBe Messages("results.not.available.yet.sub-header2")
		}

		"contain a back link pointing to /" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("href") should include("/")
		}

		"contain a choose something else to do button" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
		}

		"contain a choose something else to do button that points to /" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else").attr("href") should include("/")
		}

		"contain the correct ga events" in {
			val result = resultsNotAvailableYetView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:Results are still being added:Back"
			doc(result).getElementById("choose-something-else").attr("data-journey-click") shouldBe "button - click:Results are still being added:Choose something else to do"
		}
	}
}
