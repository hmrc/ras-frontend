/*
 * Copyright 2022 HM Revenue & Customs
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

package validators


import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import models.{MemberDateOfBirth, RasDate}
import org.joda.time.DateTime
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

trait DateValidator {

  val YEAR_FIELD_LENGTH: Int = 4
  val YEAR = "year"
  val MONTH = "month"
  val DAY = "day"

  def dateFieldConstraint(name: String): Constraint[RasDate] = Constraint("")({
    date =>
      val day = date.day.collect { case x if x.trim.nonEmpty => x }
      val month = date.month.collect { case x if x.trim.nonEmpty => x }
      val year = date.year.collect { case x if x.trim.nonEmpty => x }

      (day, month, year) match {
        case (None, None, None) => Invalid(Seq(ValidationError("error.dob.missing")))

        case (None, None, Some(_)) => Invalid(Seq(ValidationError("error.dob.missing.day.month")))
        case (Some(_), None, None) => Invalid(Seq(ValidationError("error.dob.missing.month.year")))
        case (None, Some(_), None) => Invalid(Seq(ValidationError("error.dob.missing.day.year")))

        case (None, Some(_), Some(_)) => Invalid(Seq(ValidationError("error.withName.mandatory.date", "day")))
        case (Some(_), None, Some(_)) => Invalid(Seq(ValidationError("error.withName.mandatory.date", "month")))
        case (Some(_), Some(_), None) => Invalid(Seq(ValidationError("error.withName.mandatory.date", "year")))

        case (Some(d), _, _) if !d.forall(_.isDigit) => Invalid(Seq(ValidationError("error.date.non.number.date")))
        case (_, Some(m), _) if !m.forall(_.isDigit) => Invalid(Seq(ValidationError("error.date.non.number.date")))
        case (_, _, Some(y)) if !y.forall(_.isDigit) => Invalid(Seq(ValidationError("error.date.non.number.date")))

        case _ => Valid
      }

  })

  def rasDateConstraint(name: String): Constraint[MemberDateOfBirth] = Constraint("dateOfBirth")({
    memDob => {
      val date = memDob.dateOfBirth

      val leapYear =
        try {
          new DateTime().withYear(date.year.getOrElse("0").toInt).year.isLeap
        } catch {
          case e: NumberFormatException => false
        }

      if (!DateValidator.checkDayRange(date)) {
        if (date.month.getOrElse("0").toInt == 2 && leapYear)
          Invalid(Seq(ValidationError("error.day.invalid.feb.leap", DAY)))
        else if (date.month.getOrElse("0").toInt == 2)
          Invalid(Seq(ValidationError("error.day.invalid.feb", DAY)))
        else if (List(4, 6, 9, 11).contains(date.month.getOrElse("0").toInt))
          Invalid(Seq(ValidationError("error.day.invalid.thirty", DAY)))
        else
          Invalid(Seq(ValidationError("error.day.invalid", DAY)))
      }

      else if (!DateValidator.checkMonthRange(date.month.getOrElse("0")))
        Invalid(Seq(ValidationError("error.month.invalid", MONTH)))

      else if (!DateValidator.checkYearLength(date.year.getOrElse("0")))
        Invalid(Seq(ValidationError("error.year.invalid.format", YEAR)))

      else {
        try {
          if (date.isInFuture) {
            Invalid(Seq(ValidationError("error.dob.invalid.future")))
          }
          else if (!DateValidator.isAfter1900(date.year.getOrElse("0")))
            Invalid(Seq(ValidationError("error.dob.before.1900")))
          else
            Valid
        }
        catch {
          // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
          case e: Exception => Valid
          // $COVERAGE-ON$

        }
      }
    }
  })

  def checkDayRange(date: RasDate): Boolean = {

    try {

      val day = date.day.getOrElse("0")
      val month = date.month.getOrElse("0")
      val year = date.year.getOrElse("0").toInt
      val leapYear = new DateTime().withYear(year).year.isLeap

      if (day forall Character.isDigit) {
        if (month.toInt == 2 && leapYear)
          day.toInt > 0 && day.toInt < 30
        else if (month.toInt == 2)
          day.toInt > 0 && day.toInt < 29
        else if (List(4, 6, 9, 11).contains(month.toInt))
          day.toInt > 0 && day.toInt < 31
        else
          day.toInt > 0 && day.toInt < 32
      }
      else
        false
    } catch {
      case e: NumberFormatException => false
    }

  }

  def checkMonthRange(month: String): Boolean = {
    if (month forall Character.isDigit)
      month.toInt > 0 && month.toInt < 13
    else
      false
  }

  def checkYearLength(year: String): Boolean = {
    if (year forall Character.isDigit)
      year.length == YEAR_FIELD_LENGTH
    else
      false
  }

  def isAfter1900(year: String): Boolean = {
    if (year forall Character.isDigit)
      year.toInt >= 1900
    else
      false
  }
}

object DateValidator extends DateValidator
