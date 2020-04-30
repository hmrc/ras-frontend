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

import helpers.I18nHelper
import models.{MemberDateOfBirth, MemberName, RasDate}
import play.api.data.Form
import play.api.data.Forms._
import validators.DateValidator

object MemberDateOfBirthForm extends I18nHelper {

  def apply(name: Option[String] = None) = Form(
    "dateOfBirth" -> mapping(
      "" -> mapping(
        "day" -> optional(text)
          .verifying(Messages("error.withName.mandatory.date", name.getOrElse(Messages("member")), "day"), mandatoryCheck)
          .verifying(Messages("error.date.non.number.date", name.getOrElse(Messages("member")), "day"), mandatoryCheckNonNumber),
        "month" -> optional(text)
          .verifying(Messages("error.withName.mandatory.date", name.getOrElse(Messages("member")), "month"), mandatoryCheck)
          .verifying(Messages("error.date.non.number.date", name.getOrElse(Messages("member")), "month"), mandatoryCheckNonNumber),
        "year" -> optional(text)
          .verifying(Messages("error.withName.mandatory.date", name.getOrElse(Messages("member")), "year"), mandatoryCheck)
          .verifying(Messages("error.date.non.number.date", name.getOrElse(Messages("member")), "year"), mandatoryCheckNonNumber)
      )(RasDate.apply)(RasDate.unapply)
    )(MemberDateOfBirth.apply)(MemberDateOfBirth.unapply).verifying(DateValidator.rasDateConstraint(name.getOrElse(Messages("member"))))
  )

  val mandatoryCheck: Option[String] => Boolean = input => input.getOrElse("").trim != ""
  val mandatoryCheckNonNumber: Option[String] => Boolean = input => input.getOrElse("0") forall Character.isDigit

}