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

import akka.util.Helpers.Requiring
import forms.MemberDateOfBirthForm
import models.MemberDateOfBirth
import org.scalatest.Matchers.{convertToAnyShouldWrapper, include}
import play.api.data.Form
import play.api.i18n.Messages
import org.scalatest.WordSpecLike
import utils.RasTestHelper

class MemberDOBViewSpec extends WordSpecLike with RasTestHelper {

	val memberDOBForm:Form[MemberDateOfBirth] = MemberDateOfBirthForm(Some("Jackie Chan")).bind(Map("dateOfBirth.day" -> "1", "dateOfBirth.month" -> "1", "dateOfBirth.year" -> "2000"))

	"member dob page" must {
		"contain correct page elements and content" when {
			"rendered" in {
				val result = memberDobView(memberDOBForm, "Jackie Chan", edit = false)(fakeRequest, testMessages, mockAppConfig)
				doc(result).title shouldBe Messages("member.dob.page.title")
				doc(result).getElementsByClass("govuk-fieldset__legend--xl").text shouldBe Messages("member.dob.page.header", "Jackie Chan")
				doc(result).getElementsByClass("govuk-hint").text shouldBe Messages("dob.hint")
				doc(result).getElementById("continue").text shouldBe Messages("continue")
				doc(result).getElementById("dateOfBirth.day").previousElementSibling().text() shouldBe "Day"
				doc(result).getElementById("dateOfBirth.month").previousElementSibling().text() shouldBe "Month"
				doc(result).getElementById("dateOfBirth.year").previousElementSibling().text() shouldBe "Year"
			}

			"contain the correct ga data when edit mode is false" in {
				val result = memberDobView(memberDOBForm, "Jackie Chan", edit = false)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:What is their DOB?:Continue"
				doc(result).getElementsByClass("govuk-back-link").attr("data-journey-click") shouldBe "navigation - link:What is their DOB?:Back"
			}

			"contain the correct ga data when edit mode is true" in {
				val result = memberDobView(memberDOBForm, "Jackie Chan", edit = true)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:What is their DOB?:Continue and submit"
			}
		}

		"fill in form" when {
			"details returned from session cache" in {
				val result = memberDobView(memberDOBForm, "Jackie Chan", edit = false)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("dateOfBirth.year").value.toString should include("2000")
				doc(result).getElementById("dateOfBirth.month").value.toString should include("1")
				doc(result).getElementById("dateOfBirth.day").value.toString should include("1")
			}
		}

		"present empty form" when {
			"no details returned from session cache" in {
				val emptyForm:Form[MemberDateOfBirth] = MemberDateOfBirthForm(Some("Jackie Chan")).bind(Map("dateOfBirth.day" -> "", "dateOfBirth.month" -> "", "dateOfBirth.year" -> ""))

				val result = memberDobView(emptyForm, "Jackie Chan", edit = false)(fakeRequest, testMessages, mockAppConfig)
				assert(doc(result).getElementById("dateOfBirth.year").attr("value").isEmpty)
				assert(doc(result).getElementById("dateOfBirth.month").attr("value").isEmpty)
				assert(doc(result).getElementById("dateOfBirth.day").attr("value").isEmpty)
			}
		}
	}
}
