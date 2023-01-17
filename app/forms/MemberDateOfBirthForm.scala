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

import models.{MemberDateOfBirth, RasDate}
import play.api.data.Form
import play.api.data.Forms._
import validators.DateValidator

object MemberDateOfBirthForm extends DateValidator {

	val nonNumberErrorKey = "error.date.non.number.date"
	def apply(name: Option[String] = None) = Form(
		"dateOfBirth" -> mapping(
			"" -> mapping(
				"day" -> optional(text)
					.verifying("error.day.missing", mandatoryCheck)
					.verifying(nonNumberErrorKey, mandatoryCheckNonNumber),
				"month" -> optional(text)
					.verifying("error.month.missing", mandatoryCheck)
					.verifying(nonNumberErrorKey, mandatoryCheckNonNumber),
				"year" -> optional(text)
					.verifying("error.year.missing", mandatoryCheck)
					.verifying(nonNumberErrorKey, mandatoryCheckNonNumber)
			)(RasDate.apply)(RasDate.unapply)

		)(MemberDateOfBirth.apply)(MemberDateOfBirth.unapply)

			.verifying(rasDateConstraint(name.getOrElse("member")))
	)

	val mandatoryCheck: Option[String] => Boolean = input => input.getOrElse("").trim != ""
	val mandatoryCheckNonNumber: Option[String] => Boolean = input => input.getOrElse("0") forall Character.isDigit

}
