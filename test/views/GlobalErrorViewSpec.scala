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

import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers.contentAsString
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper
import play.api.test.Helpers._


class GlobalErrorViewSpec extends UnitSpec with RasTestHelper {

	"global error page" should {

		"contain correct title and header" in {
			val result = views.html.global_error()(fakeRequest, testMessages, mockAppConfig)
			val doc = Jsoup.parse(contentAsString(result))
			doc.title shouldBe Messages("global.error.page.title")
			doc.getElementById("header").text shouldBe Messages("global.error.header")
			doc.getElementById("message").text shouldBe Messages("global.error.message")
		}
	}
}
