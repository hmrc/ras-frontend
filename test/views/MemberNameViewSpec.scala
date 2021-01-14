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
			val result = views.html.member_name(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("member.name.page.title")
			doc(result).getElementById("header").text shouldBe Messages("member.name.page.header")
		}

		"contain correct field labels" in {
			val result = views.html.member_name(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("firstName_label").text shouldBe "First name"
			doc(result).getElementById("lastName_label").text shouldBe "Last name"
		}

		"contain correct input fields" in {
			val result = views.html.member_name(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			assert(doc(result).getElementById("firstName").attr("input") != null)
			assert(doc(result).getElementById("lastName").attr("input") != null)
		}

		"contain continue button" in {
			val result = views.html.member_name(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("continue").text shouldBe Messages("continue")
		}

		"fill in form if cache data is returned" in {
			val result = views.html.member_name(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("firstName").value.toString should include(memberName.firstName)
			doc(result).getElementById("lastName").value.toString should include(memberName.lastName)
		}

		"present empty form when no cached data exists" in {
			val emptyForm:Form[MemberName] = MemberNameForm.form.bind(Map("firstName" -> "", "lastName" -> ""))

			val result = await(views.html.member_name(emptyForm, edit = false)(fakeRequest, testMessages, mockAppConfig))
			assert(doc(result).getElementById("firstName").attr("value").equals(""))
			assert(doc(result).getElementById("lastName").attr("value").equals(""))
		}

		"contain the correct ga data when edit mode is false" in {
			val result = await(views.html.member_name(memberNameForm, edit = false)(fakeRequest, testMessages, mockAppConfig))
			assert(doc(result).getElementById("continue").attr("data-journey-click").equals("button - click:What is their name?:Continue"))
			assert(doc(result).getElementsByClass("link-back").attr("data-journey-click").equals("navigation - link:What is their name?:Back"))
		}

		"contain the correct ga data when edit mode is true" in {
			val result = await(views.html.member_name(memberNameForm, edit = true)(fakeRequest, testMessages, mockAppConfig))
			assert(doc(result).getElementById("continue").attr("data-journey-click").equals("button - click:What is their name?:Continue and submit"))
		}
	}

}
