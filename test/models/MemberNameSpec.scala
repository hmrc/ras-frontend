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

package models

import org.scalatest.WordSpecLike

class MemberNameSpec extends WordSpecLike {

  "hasValue" must {

    "return false if first name is empty" in {
      val name = MemberName("", "last name")
      assert(!name.hasAValue)
    }

    "return false if firstName & lastName are empty" in {
      val name = MemberName ("", "")
      assert(!name.hasAValue)
    }

    "return true if firstName & lastName contain values" in {
      val name = MemberName ("Jim", "Jimson")
      assert(name.hasAValue)
    }
  }
}
