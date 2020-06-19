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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import forms.{MemberDateOfBirthForm => form}
import models.RasDate
import org.joda.time.LocalDate
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

class MemberDateOfBirthFormSpec extends UnitSpec with RasTestHelper {

  "Member date of birth form" should {

    "return no error when valid data is entered" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("1"), Some("1"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.isEmpty)
    }

    "return error when all fields are empty" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(None, None, None))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.day", List("error.withName.mandatory.date"), Seq("Chris Bristow", "day"))))
      assert(validatedForm.errors.contains(FormError("dateOfBirth.month", List("error.withName.mandatory.date"), Seq("Chris Bristow", "month"))))
      assert(validatedForm.errors.contains(FormError("dateOfBirth.year", List("error.withName.mandatory.date"), Seq("Chris Bristow", "year"))))

    }

    "return error when day is empty" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(None, Some("1"), Some("1999")))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.day", List("error.withName.mandatory.date"), Seq("Chris Bristow", "day"))))
    }

    "return error when month is empty" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("1"), None, Some("1999")))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.month", List("error.withName.mandatory.date"), Seq("Chris Bristow", "month"))))
    }

    "return error when year is empty" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("2"), Some("1"), None))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.year", List("error.withName.mandatory.date"), Seq("Chris Bristow", "year"))))
    }

    "return error when all fields are not a number" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("a"), Some("b"), Some("!")))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.day", List("error.date.non.number.date"), Seq("Chris Bristow", "day"))))
      assert(validatedForm.errors.contains(FormError("dateOfBirth.month", List("error.date.non.number.date"), Seq("Chris Bristow", "month"))))
      assert(validatedForm.errors.contains(FormError("dateOfBirth.year", List("error.date.non.number.date"), Seq("Chris Bristow", "year"))))
    }

    "return error when day is not a number" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("a"), Some("2"), Some("1")))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.day", List("error.date.non.number.date"), Seq("Chris Bristow", "day"))))
    }

    "return error when month is not a number" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("1"), Some("a"), Some("1")))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.month", List("error.date.non.number.date"), Seq("Chris Bristow", "month"))))
    }

    "return error when year is not a number" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("2"), Some("2"), Some("a")))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth.year", List("error.date.non.number.date"), Seq("Chris Bristow", "year"))))
    }

    "return error when non existing date is entered in month 2" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("29"), Some("2"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid.feb"), Seq("day"))))
    }

    "return error when non existing date is entered in month 2 and leap year" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("30"), Some("2"), Some("2056")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid.feb.leap"), Seq("day"))))
    }

    "return error when non existing date is entered in month 4" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("31"), Some("4"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid.thirty"), Seq("day"))))
    }

    "return error when non existing date is entered in month 6" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("31"), Some("6"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid.thirty"), Seq("day"))))
    }

    "return error when non existing date is entered in month 9" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("31"), Some("9"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid.thirty"), Seq("day"))))
    }

    "return error when non existing date is entered in month 11" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("31"), Some("11"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid.thirty"), Seq("day"))))
    }

    "return error when day over 31 is entered" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("32"), Some("3"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid"), Seq("day"))))
    }

    "return error when day under one is entered is entered" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("0"), Some("3"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.day.invalid"), Seq("day"))))
    }

    "return error when 0 is entered for month" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("2"), Some("0"), Some("1999")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.month.invalid"), Seq("month"))))
    }

    "return error when 0 is entered for year" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("2"), Some("3"), Some("0")))
      val validatedForm = form().bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.year.invalid.format"), Seq("year"))))
    }

    "return error when date is in future" in {
      val year = (LocalDate.now.getYear + 1).toString
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("2"), Some("3"), Some(year)))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      val nextDay = DateTimeFormatter.ofPattern("dd MMMM uuuu").format(LocalDateTime.now().plusDays(1))
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.dob.invalid.future"), Seq("Chris Bristow", "date of birth", nextDay, "day"))))
    }

    "return error when date is before 1900" in {
      val formData = Json.obj("dateOfBirth" -> RasDate(Some("2"), Some("3"), Some("1899")))
      val validatedForm = form(Some("Chris Bristow")).bind(formData)
      assert(validatedForm.errors.contains(FormError("dateOfBirth", List("error.dob.before.1900"), Seq( "Chris Bristow", "date of birth", "year"))))
    }
  }
}
