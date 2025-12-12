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

import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, include}
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, _}
import utils.RasTestHelper

class SorryYouNeedToStartAgainViewSpec extends AnyWordSpec with RasTestHelper {

	"sorry you need to start again page" must {

		"contain correct title and header" in {
			val result = startAtStartView()(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))

			doc.title shouldBe Messages("sorry.you.need.to.start.again.title")
			doc.getElementById("header").text shouldBe Messages("you.need.to.start.again")
		}

		"contain correct body text and list items" in {
			val result = startAtStartView()(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))

			doc.getElementsByClass("govuk-body").first().text shouldBe Messages("you.can.either")

			val listItems = doc.getElementsByClass("govuk-list--bullet").first().getElementsByTag("li")
			listItems.size() shouldBe 2

			listItems.get(0).text should include(Messages("start.again.first.link"))
			listItems.get(0).text should include(Messages("start.again.first.list"))

			listItems.get(1).text should include(Messages("start.again.second.link"))
			listItems.get(1).text should include(Messages("start.again.second.list"))
		}
	}
}
