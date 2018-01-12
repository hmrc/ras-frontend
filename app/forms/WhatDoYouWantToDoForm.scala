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

import helpers.helpers.I18nHelper
import models.WhatDoYouWantToDo
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

object WhatDoYouWantToDoForm extends I18nHelper{

  val whatDoYouWantToDoForm = Form(
    mapping(
      "userChoice" ->
        optional(text).verifying("have to select an option", {choice => {choice.isDefined && choice.get.matches("[1-3]{1}")}})
    )(WhatDoYouWantToDo.apply)(WhatDoYouWantToDo.unapply)
  )

}
