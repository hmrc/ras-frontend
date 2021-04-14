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

class NotAuthorisedViewSpec extends UnitSpec with RasTestHelper {

	"not authorised page" should {
		"contain the correct page title" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("unauthorised.error.page.title")
		}

		"contain the correct page header" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("header").text shouldBe Messages("unauthorised.page.header")
		}

		"contain the correct top paragraph" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("paragraph-top").text shouldBe Messages("unauthorised.paragraph.top")
		}

		"contain the correct bottom paragraph" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("paragraph-bottom").text shouldBe Messages("unauthorised.paragraph.bottom")
		}

		"contain a list with two items" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("action-list").children().size() shouldBe 2
		}

		"first list item should contain the correct text" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("action-list").children().first().text shouldBe Messages("unauthorised.list.first", Messages("unauthorised.list.first.link"))
		}

		"first list item link should have the correct href" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("link-sign-in").attr("href") shouldBe mockAppConfig.signOutAndContinueUrl
		}

		"second list item should contain the correct text" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("action-list").children().last().text shouldBe Messages("unauthorised.list.last")
		}

		"second list item link should have the correct href" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("link-register").attr("href") shouldBe "https://online.hmrc.gov.uk/registration/pensions"
		}

		"first list item link should have the correct ga event" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("link-sign-in").attr("data-journey-click") shouldBe "link - click:There is a problem:Sign in"
		}

		"second list item link should have the correct ga event" in {
			val result = unauthorisedView()(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("link-register").attr("data-journey-click") shouldBe "link - click:There is a problem:Register"
		}
	}

}
