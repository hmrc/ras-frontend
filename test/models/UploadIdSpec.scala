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

import models.upscan.UploadId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.QueryStringBindable

import java.util.UUID

class UploadIdSpec extends AnyWordSpec with Matchers {

  private val binder = implicitly[QueryStringBindable[UploadId]]

  "generate" should {
    "return with uuid" in {

      val actual = UploadId.generate

      noException should be thrownBy {
        UUID.fromString(actual.value)
      }

    }
  }

  "QueryBinder" should {
    "bind and unbind UploadId" in {
      val original = UploadId("bind-test-123")

      val bound =
        binder.bind("uploadId", Map("uploadId" -> Seq(original.value)))

      bound shouldBe Some(Right(original))

      binder.unbind("uploadId", original) shouldBe s"uploadId=${original.value}"
    }

  }
}
