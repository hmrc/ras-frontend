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

import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper


class MatchNotFoundViewSpec extends UnitSpec with RasTestHelper {

	val nino: String = "AA123456A"
	val dob: String = new LocalDate(1999, 1, 1).toString("d MMMM yyyy")

	"match not found page" should {
		"contain correct title when match not found" in {
			val result = views.html.match_not_found("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))
			doc.title shouldBe Messages("match.not.found.page.title")
		}

		"contain customer details and residency status when match not found" in {
			val result = views.html.match_not_found("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("match-not-found").text shouldBe Messages("member.details.not.found", "Jim McGill")
			doc(result).getElementById("subheader").text shouldBe Messages("match.not.found.subheader","Jim McGill")
			doc(result).getElementById("change-name").text shouldBe Messages("change.name") + " " + Messages("change")
			doc(result).getElementById("name").text shouldBe "Jim McGill"
			doc(result).getElementById("change-nino").text shouldBe Messages("change.nino") + " " + Messages("change")
			doc(result).getElementById("nino").text shouldBe nino
			doc(result).getElementById("change-dob").text shouldBe Messages("change.dob") + " " + Messages("change")
			doc(result).getElementById("dob").text shouldBe dob
			doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
		}

		"contain a member must contact HMRC to update their personal details link which opens a new tab when clicked" in {
			val result = views.html.match_not_found("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("contact-hmrc-link").attr("href") shouldBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/national-insurance-numbers"
			doc(result).getElementById("contact-hmrc-link").attr("target") shouldBe "_blank"
		}

		"contain what to do next section when match not found" in {
			val result = views.html.match_not_found("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("what-to-do").text shouldBe Messages("match.not.found.what.to.do", Messages("contact.hmrc", "Jim McGill"))
		}

		"contain a look up another member link when match not found" in {
			val result = views.html.match_not_found("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("look-up-another-member-link").attr("href") shouldBe "/relief-at-source/check-another-member/member-name?cleanSession=true"
		}

		"contain ga event data when match not found " in {
			val result = views.html.match_not_found("Jim McGill", dob, nino)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:Users details not found:Back"
			doc(result).getElementById("change-name-link").attr("data-journey-click") shouldBe "link - click:User details not found:Change Name"
			doc(result).getElementById("change-nino-link").attr("data-journey-click") shouldBe "link - click:User details not found:Change NINO"
			doc(result).getElementById("change-dob-link").attr("data-journey-click") shouldBe "link - click:User details not found:Change DOB"
			doc(result).getElementById("choose-something-else").attr("data-journey-click") shouldBe "button - click:User details not found:Choose something else to do"
			doc(result).getElementById("look-up-another-member-link").attr("data-journey-click") shouldBe "link - click:User details not found:Look up another member"
		}
	}
}
