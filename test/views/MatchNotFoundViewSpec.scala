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

import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, _}
import org.scalatest.WordSpecLike
import utils.RasTestHelper
import views.helpers.ViewSpecHelper


class MatchNotFoundViewSpec extends ViewSpecHelper {

	val nino: String = "AA123456A"
	val dob: String = new LocalDate(1999, 1, 1).toString("d MMMM yyyy")

	"match not found page" must {

		behave like pageWithFeedbackLink(matchNotFoundView("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig))

		"contain correct title when match not found" in {
			val result = matchNotFoundView("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))
			doc.title shouldBe Messages("match.not.found.page.title")
		}

		"contain customer details and residency status when match not found" in {
			val result = matchNotFoundView("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("match-not-found").text shouldBe Messages("member.details.not.found", "Jim McGill")
			doc(result).getElementsByClass("govuk-body-l").text shouldBe Messages("match.not.found.subheader","Jim McGill")
			doc(result).getElementById("change-name-link").text shouldBe Messages("change") + " " + Messages("change.name")
			doc(result).getElementsByClass("govuk-summary-list__value").get(0).text shouldBe "Jim McGill"
			doc(result).getElementById("change-nino-link").text shouldBe Messages("change") + " " + Messages("change.nino")
			doc(result).getElementsByClass("govuk-summary-list__value").get(1).text shouldBe nino
			doc(result).getElementById("change-dob-link").text shouldBe Messages("change") + " " + Messages("change.dob")
			doc(result).getElementsByClass("govuk-summary-list__value").get(2).text shouldBe dob
		}

		"contain a member must contact HMRC to update their personal details link which opens a new tab when clicked" in {
			val result = matchNotFoundView("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("contact-hmrc-link").attr("href") shouldBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/national-insurance-numbers"
			doc(result).getElementById("contact-hmrc-link").attr("target") shouldBe "_blank"
		}

		"contain what to do next section when match not found" in {
			val result = matchNotFoundView("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("what-to-do").text shouldBe Messages("match.not.found.what.to.do") + " " + Messages("contact.hmrc", "Jim McGill")
		}

		"contain a look up another member link when match not found" in {
			val result = matchNotFoundView("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("look-up-another-member-link").attr("href") shouldBe "/relief-at-source/check-another-member/member-name?cleanSession=true"
		}

		"contain ga event data when match not found " in {
			val result = matchNotFoundView("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementsByClass("govuk-back-link").attr("data-journey-click") shouldBe "navigation - link:Users details not found:Back"
			doc(result).getElementById("change-name-link").attr("data-journey-click") shouldBe "link - click:User details not found:Change Name"
			doc(result).getElementById("change-nino-link").attr("data-journey-click") shouldBe "link - click:User details not found:Change NINO"
			doc(result).getElementById("change-dob-link").attr("data-journey-click") shouldBe "link - click:User details not found:Change DOB"
			doc(result).getElementById("look-up-another-member-link").attr("data-journey-click") shouldBe "link - click:User details not found:Look up another member"
		}
	}
}
