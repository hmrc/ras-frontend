/*
 * Copyright 2026 HM Revenue & Customs
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

import forms.MemberNinoForm
import models.MemberNino
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.Form
import play.api.i18n.Messages
import utils.RasTestHelper

class MemberNinoViewSpec extends AnyWordSpec with RasTestHelper {

	val memberNinoForm:Form[MemberNino] = MemberNinoForm(Some("John Doe")).bind(Map("nino" -> "AA123456A"))

	"member nino page" must {
		"contain correct page elements and content" when {
			"rendered" in {
				val result = memberNinoView(memberNinoForm, "Jackie Chan", edit = false)(fakeRequest, testMessages, mockAppConfig)
				doc(result).title shouldBe Messages("member.nino.page.title")
				doc(result).getElementsByClass("govuk-fieldset__legend--xl").text shouldBe Messages("member.nino.page.header", "Jackie Chan")
				doc(result).getElementsByClass("govuk-hint").text shouldBe Messages("nino.hint")
				assert(doc(result).getElementById("nino").attr("input") != null)
				doc(result).getElementById("continue").text shouldBe Messages("continue")
			}

			"rendered but no cached data exists" in {
				val result = memberNinoView(memberNinoForm, "member", edit = false)(fakeRequest, testMessages, mockAppConfig)
				doc(result).title shouldBe Messages("member.nino.page.title")
				doc(result).getElementsByClass("govuk-fieldset__legend--xl").text shouldBe Messages("member.nino.page.header", Messages("member"))
			}

			"contain the correct ga data when edit mode is false" in {
				val result = memberNinoView(memberNinoForm, "John Doe", edit = false)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:What is their NINO?:Continue"
				doc(result).getElementsByClass("govuk-back-link").attr("data-journey-click") shouldBe "navigation - link:What is their NINO?:Back"
			}

			"contain the correct ga data when edit mode is true" in {
				val result = memberNinoView(memberNinoForm, "John Doe", edit = true)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:What is their NINO?:Continue and submit"
			}

			"present empty form when no cached data exists" in {
				val emptyForm:Form[MemberNino] = MemberNinoForm(Some("John Doe")).bind(Map("nino" -> ""))

				val result = memberNinoView(emptyForm, "John Doe", edit = false)(fakeRequest, testMessages, mockAppConfig)
				assert(doc(result).getElementById("nino").attr("value").equals(""))
			}
		}
	}
}
