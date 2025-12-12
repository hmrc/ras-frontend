/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class QuestionnaireSpec extends AnyWordSpec with Matchers {

  "Questionnaire" should {

    "create a valid Questionnaire instance" in {
      val questionnaire = Questionnaire(
        easyToUse = 3,
        satisfactionLevel = 4,
        whyGiveThisRating = Some("Great service"),
        referer = Some("https://www.gov.uk")
      )

      questionnaire.easyToUse shouldBe 3
      questionnaire.satisfactionLevel shouldBe 4
      questionnaire.whyGiveThisRating shouldBe Some("Great service")
      questionnaire.referer shouldBe Some("https://www.gov.uk")
    }

    "bind valid form data" in {
      val formData = Map(
        "easyToUse" -> "2",
        "satisfactionLevel" -> "3",
        "whyGiveThisRating" -> "Good experience",
        "referer" -> "https://example.com"
      )

      val boundForm = Questionnaire.form.bind(formData)

      boundForm.hasErrors shouldBe false
      boundForm.value shouldBe Some(Questionnaire(2, 3, Some("Good experience"), Some("https://example.com")))
    }

    "bind form data with optional fields empty" in {
      val formData = Map(
        "easyToUse" -> "1",
        "satisfactionLevel" -> "2"
      )

      val boundForm = Questionnaire.form.bind(formData)

      boundForm.hasErrors shouldBe false
      boundForm.value shouldBe Some(Questionnaire(1, 2, None, None))
    }

    "reject easyToUse value above max" in {
      val formData = Map(
        "easyToUse" -> "5",
        "satisfactionLevel" -> "2"
      )

      val boundForm = Questionnaire.form.bind(formData)

      boundForm.hasErrors shouldBe true
      boundForm.errors.head.key shouldBe "easyToUse"
    }

    "reject satisfactionLevel value above max" in {
      val formData = Map(
        "easyToUse" -> "2",
        "satisfactionLevel" -> "5"
      )

      val boundForm = Questionnaire.form.bind(formData)

      boundForm.hasErrors shouldBe true
      boundForm.errors.head.key shouldBe "satisfactionLevel"
    }

    "reject whyGiveThisRating exceeding max length" in {
      val longString = "a" * (Questionnaire.maxStringLength + 1)
      val formData = Map(
        "easyToUse" -> "2",
        "satisfactionLevel" -> "3",
        "whyGiveThisRating" -> longString
      )

      val boundForm = Questionnaire.form.bind(formData)

      boundForm.hasErrors shouldBe true
      boundForm.errors.head.key shouldBe "whyGiveThisRating"
    }
  }
}