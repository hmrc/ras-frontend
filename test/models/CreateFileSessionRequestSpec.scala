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
import play.api.libs.json.{JsError, JsSuccess, Json}

class CreateFileSessionRequestSpec extends AnyWordSpec with Matchers {

  private val userId = "user123"
  private val reference = "ref456"
  private val alternateUserId = "user789"
  private val alternateReference = "ref012"
  private val emptyString = ""

  "CreateFileSessionRequest" should {

    "create a CreateFileSessionRequest instance" in {
      val request = CreateFileSessionRequest(userId, reference)

      request.userId shouldBe userId
      request.reference shouldBe reference
    }

    "serialize to JSON" in {
      val request = CreateFileSessionRequest(userId, reference)

      val json = Json.toJson(request)

      (json \ "userId").as[String] shouldBe userId
      (json \ "reference").as[String] shouldBe reference
    }

    "deserialize from valid JSON" in {
      val json = Json.obj(
        "userId" -> alternateUserId,
        "reference" -> alternateReference
      )

      val result = Json.fromJson[CreateFileSessionRequest](json)

      result shouldBe a[JsSuccess[_]]
      val request = result.get
      request.userId shouldBe alternateUserId
      request.reference shouldBe alternateReference
    }

    "reject JSON with empty userId" in {
      val json = Json.obj(
        "userId" -> emptyString,
        "reference" -> reference
      )

      val result = Json.fromJson[CreateFileSessionRequest](json)

      result shouldBe a[JsError]
      result.asInstanceOf[JsError].errors.head._1.toString() should include("userId")
    }

    "reject JSON with empty reference" in {
      val json = Json.obj(
        "userId" -> userId,
        "reference" -> emptyString
      )

      val result = Json.fromJson[CreateFileSessionRequest](json)

      result shouldBe a[JsError]
      result.asInstanceOf[JsError].errors.head._1.toString() should include("reference")
    }

    "reject JSON with both fields empty" in {
      val json = Json.obj(
        "userId" -> emptyString,
        "reference" -> emptyString
      )

      val result = Json.fromJson[CreateFileSessionRequest](json)

      result shouldBe a[JsError]
    }
  }
}