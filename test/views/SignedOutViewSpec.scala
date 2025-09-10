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
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import utils.RasTestHelper

class SignedOutViewSpec extends AnyWordSpec with RasTestHelper {

  "signed out page" must {

    "contain correct title and sign in button should have correct href" in {
      val result = signedOutView()(fakeRequest, testMessages, mockAppConfig)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("sign.out.page.title")
      doc.getElementById("sign in").attr("href") shouldBe mockAppConfig.signOutAndContinueUrl
    }
  }
}
