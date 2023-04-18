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

import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.RasTestHelper


class GlobalErrorViewSpec extends AnyWordSpec with RasTestHelper {

	"global error page" must {

		"contain correct title and header" in {
			val result = globalErrorView()(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))
			doc.title shouldBe Messages("global.error.page.title")
			doc.getElementsByClass("govuk-heading-xl").text shouldBe Messages("global.error.header")
			doc.getElementsByClass("govuk-body").text shouldBe Messages("you.can.either")
		}
	}
}
