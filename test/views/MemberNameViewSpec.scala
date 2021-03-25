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

import forms.MemberNameForm
import models.MemberName
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

class MemberNameViewSpec extends UnitSpec with RasTestHelper {

	val memberName = MemberName("Jackie", "Chan")
	val memberNameForm:Form[MemberName] = MemberNameForm.form.bind(Map("firstName" -> "Jackie", "lastName" -> "Chan"))

	"member name page" should {

		"contain correct title and header" in {
			val result = memberNameView(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("member.name.page.title")
			doc(result).getElementsByClass("govuk-fieldset__legend--xl").text shouldBe Messages("member.name.page.header")
		}

		"contain correct field labels" in {
			val result = memberNameView(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementsByClass("govuk-label").get(0).text shouldBe "First name line 1 of 2"
			doc(result).getElementsByClass("govuk-label").get(1).text shouldBe "Last name line 2 of 2"
		}

		"contain correct input fields" in {
			val result = memberNameView(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			assert(doc(result).getElementById("firstName").attr("input") != null)
			assert(doc(result).getElementById("lastName").attr("input") != null)
		}

		"contain continue button" in {
			val result = memberNameView(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("continue").text shouldBe Messages("continue")
		}

		"fill in form if cache data is returned" in {
			val result = memberNameView(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("firstName").value.toString should include(memberName.firstName)
			doc(result).getElementById("lastName").value.toString should include(memberName.lastName)
		}

		"present empty form when no cached data exists" in {
			val emptyForm:Form[MemberName] = MemberNameForm.form.bind(Map("firstName" -> "", "lastName" -> ""))

			val result = await(memberNameView(emptyForm, edit = false)(fakeRequest, testMessages, mockAppConfig))
			assert(doc(result).getElementById("firstName").attr("value").equals(""))
			assert(doc(result).getElementById("lastName").attr("value").equals(""))
		}

		"contain the correct ga data when edit mode is false" in {
			val result = await(memberNameView(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig))
			assert(doc(result).getElementById("continue").attr("data-journey-click").equals("button - click:What is their name?:Continue"))
			assert(doc(result).getElementsByClass("govuk-back-link").attr("data-journey-click").equals("navigation - link:What is their name?:Back"))
		}

		"contain the correct ga data when edit mode is true" in {
			val result = await(memberNameView(memberNameForm, edit = true)(fakeRequest, testMessages, mockAppConfig))
			assert(doc(result).getElementById("continue").attr("data-journey-click").equals("button - click:What is their name?:Continue and submit"))
		}
	}

}
