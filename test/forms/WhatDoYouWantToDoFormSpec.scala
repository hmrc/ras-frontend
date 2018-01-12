/*
 * Copyright 2018 HM Revenue & Customs
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

import forms.WhatDoYouWantToDoForm._
import helpers.helpers.I18nHelper
import models.WhatDoYouWantToDo
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.FormError
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class WhatDoYouWantToDoFormSpec extends UnitSpec with I18nHelper with OneAppPerSuite {

  "What do you want to do form" should {

    "not contain return an error when an option is selected" in {
      val whatDoYouWantToDo =  Json.toJson(WhatDoYouWantToDo(Some("1")))
      val result = whatDoYouWantToDoForm.bind(whatDoYouWantToDo)
      assert(result.errors.size == 0)
      assert(!result.errors.contains(FormError("userChoice",List(Messages("select.an.answer")))))
    }

    "contain an error if no option is selected" in {
      val result = whatDoYouWantToDoForm.bind(Map[String, String]())
      assert(result.errors.size == 1)
      assert(result.errors.contains(FormError("userChoice",List(Messages("select.an.answer")))))
    }

  }
}