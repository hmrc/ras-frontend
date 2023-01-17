/*
 * Copyright 2023 HM Revenue & Customs
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

import models.MemberName
import play.api.data.Form
import play.api.data.Forms._

object MemberNameForm {

  val MAX_LENGTH = 35
  val NAME_REGEX = "^[a-zA-Z &`\\-\\'^]+$"

  val form = Form(
    mapping(
      "firstName" -> text
        .verifying("error.mandatory.firstName", _.nonEmpty)
        .verifying("error.length.firstName", _.length <= MAX_LENGTH)
        .verifying("error.firstName.invalid", x => x.isEmpty || x.matches(NAME_REGEX)),
      "lastName" -> text
        .verifying("error.mandatory.lastName", _.nonEmpty)
        .verifying("error.length.lastName", _.length <= MAX_LENGTH)
        .verifying("error.lastName.invalid", x => x.isEmpty || x.matches(NAME_REGEX))
    )(MemberName.apply)(MemberName.unapply)
  )
}
