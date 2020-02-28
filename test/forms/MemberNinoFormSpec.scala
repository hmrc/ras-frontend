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

package forms

import forms.MemberNinoForm._
import helpers.RandomNino
import helpers.I18nHelper
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.FormError
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class MemberNinoFormSpec extends UnitSpec with I18nHelper with OneAppPerSuite {

  val MAX_NAME_LENGTH = 35

  "Find member details form" should {

    "return no error when valid data is entered" in {
      val formData = Json.obj("nino" -> RandomNino.generate)
      val validatedForm = form.bind(formData)
      assert(validatedForm.errors.isEmpty)
    }

    "return an error when nino field is empty" in {
      val formData = Json.obj("nino" -> "")
      val validatedForm = form.bind(formData)
      assert(validatedForm.errors.contains(FormError("nino", List(Messages("error.mandatory", Messages("nino"))))))
    }

    "return an error when invalid nino is passed" in {
      val formData = Json.obj("nino" -> "QQ322312B")
      val validatedForm = form.bind(formData)
      assert(validatedForm.errors.contains(FormError("nino", List(Messages("error.nino.invalid")))))
      assert(!validatedForm.errors.contains(FormError("nino", List(Messages("error.mandatory")))))
    }

    "return an error when invalid nino suffix is passed" in {
      val formData = Json.obj("nino" -> "AB322312E")
      val validatedForm = form.bind(formData)
      assert(validatedForm.errors.contains(FormError("nino", List(Messages("error.nino.invalid")))))
      assert(!validatedForm.errors.contains(FormError("nino", List(Messages("error.mandatory")))))
    }

    "return no error when nino with no suffix is passed" in {
      val formData = Json.obj("nino" -> "AB123456")
      val validatedForm = form.bind(formData)
      assert(validatedForm.errors.isEmpty)
    }

  }
}
