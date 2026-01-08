/*
 * Copyright 2026 HM Revenue & Customs
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

class ApiVersionSpec extends AnyWordSpec with Matchers {

  "ApiV1_0" should {

    "return correct string representation" in {
      ApiV1_0.toString shouldBe "1.0"
    }

    "be an instance of ApiVersion" in {
      ApiV1_0 shouldBe an[ApiVersion]
    }
  }

  "ApiV2_0" should {

    "return correct string representation" in {
      ApiV2_0.toString shouldBe "2.0"
    }

    "be an instance of ApiVersion" in {
      ApiV2_0 shouldBe an[ApiVersion]
    }
  }
}