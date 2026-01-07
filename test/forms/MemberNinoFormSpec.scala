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

package forms

import forms.{MemberNinoForm => form}
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import play.api.libs.json.Json
import utils.{RandomNino, RasTestHelper}

class MemberNinoFormSpec extends AnyWordSpec with RasTestHelper {

  val MAX_NAME_LENGTH = 35
  val fromJsonMaxChars: Long = 102400

  "Find member details form" must {

    "return no error when valid data is entered" in {
      val formData = Json.obj("nino" -> RandomNino.generate)
      val validatedForm = form().bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }

    "return an error when nino field is empty" in {
      val formData = Json.obj("nino" -> "")
      val validatedForm = form(Some("James Potter")).bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("nino", List("error.withName.mandatory"), Seq("National Insurance number"))))
    }

    "return an error when nino field has special character" in {
      val formData = Json.obj("nino" -> "a!")
      val validatedForm = form(Some("James Potter")).bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("nino", List("error.nino.special.character"))))
    }

    "return an error when invalid nino is passed" in {
      val formData = Json.obj("nino" -> "QQ322312B")
      val validatedForm = form().bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("nino", List("error.nino.invalid"))))
    }

    "return an error when invalid nino suffix is passed" in {
      val formData = Json.obj("nino" -> "AB322312E")
      val validatedForm = form().bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("nino", List("error.nino.invalid"))))
    }

    "return an error when invalid nino length is passed" in {
      val formData = Json.obj("nino" -> "AA")
      val validatedForm = form().bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.contains(FormError("nino", List("error.nino.length"))))
    }

    "return no error when nino with no suffix is passed" in {
      val formData = Json.obj("nino" -> "AB123456")
      val validatedForm = form().bind(formData, fromJsonMaxChars)
      assert(validatedForm.errors.isEmpty)
    }
  }
}
