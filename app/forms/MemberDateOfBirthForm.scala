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

package forms

import models.{MemberDateOfBirth, RasDate}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import validators.DateValidator

object MemberDateOfBirthForm {

	val dateErrorKey = "error.withName.mandatory.date"
	val nonNumberErrorKey = "error.date.non.number.date"
  def apply(name: Option[String] = None) = Form(
    "dateOfBirth" -> mapping(
      "" -> mapping(
        "day" -> optional(text)
					.verifying(mandatoryCheckConstraint(dateErrorKey, mandatoryCheck, name.getOrElse("member"), "day"))
					.verifying(mandatoryCheckConstraint(nonNumberErrorKey, mandatoryCheckNonNumber, name.getOrElse("member"), "day")),
        "month" -> optional(text)
					.verifying(mandatoryCheckConstraint(dateErrorKey, mandatoryCheck, name.getOrElse("member"), "month"))
					.verifying(mandatoryCheckConstraint(nonNumberErrorKey, mandatoryCheckNonNumber, name.getOrElse("member"), "month")),
        "year" -> optional(text)
					.verifying(mandatoryCheckConstraint(dateErrorKey, mandatoryCheck, name.getOrElse("member"), "year"))
					.verifying(mandatoryCheckConstraint(nonNumberErrorKey, mandatoryCheckNonNumber, name.getOrElse("member"), "year"))
      )(RasDate.apply)(RasDate.unapply)
    )(MemberDateOfBirth.apply)(MemberDateOfBirth.unapply).verifying(DateValidator.rasDateConstraint(name.getOrElse("member")))
  )

  val mandatoryCheck: Option[String] => Boolean = input => input.getOrElse("").trim != ""
  val mandatoryCheckNonNumber: Option[String] => Boolean = input => input.getOrElse("0") forall Character.isDigit

	def mandatoryCheckConstraint(errorKey: String, constraint: Option[String] => Boolean, name: String, key: String): Constraint[Option[String]] = {
		Constraint { t: Option[String] =>
			if (constraint(t)) Valid else Invalid(Seq(ValidationError(errorKey, name, key)))
		}
	}
}
