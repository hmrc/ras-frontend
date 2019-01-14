/*
 * Copyright 2019 HM Revenue & Customs
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

import helpers.helpers.I18nHelper
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class MemberDobSpec extends UnitSpec {

  "hasValue" should {

    "return false if dateOfBirth is empty" in {
        val dob = MemberDateOfBirth(RasDate(None, None, None))
        assert(dob.hasAValue() == false)
    }

    "return true if dateOfBirth contains values" in {
      val dob = MemberDateOfBirth(RasDate(Some("1"), Some("1"), Some("1990")))
      assert(dob.hasAValue() == true)
    }
  }
}