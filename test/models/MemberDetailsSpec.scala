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
import play.api.libs.json.{JsSuccess, Json}


class MemberDetailsSpec extends AnyWordSpec with Matchers {

  val memberName: MemberName = MemberName("John", "Smith")
  val rasDate: RasDate = RasDate(Some("1"), Some("2"), Some("1990"))
  val nino = "AB123456C"

  "MemberDetails" should {

    "create a MemberDetails instance" in {
      val memberDetails = MemberDetails(memberName, nino, rasDate)

      memberDetails.name shouldBe memberName
      memberDetails.nino shouldBe nino
      memberDetails.dateOfBirth shouldBe rasDate
    }

    "convert to customer details payload with uppercase NINO and capitalised names" in {
      val memberDetails = MemberDetails(
        MemberName("john", "smith"),
        "ab123456c",
        RasDate(Some("15"), Some("3"), Some("1990"))
      )

      val payload = memberDetails.asCustomerDetailsPayload

      (payload \ "nino").as[String] shouldBe "AB123456C"
      (payload \ "firstName").as[String] shouldBe "John"
      (payload \ "lastName").as[String] shouldBe "Smith"
      (payload \ "dateOfBirth").as[String] shouldBe "1990-03-15"
    }

    "serialize to JSON with uppercase NINO" in {
      val memberDetails = MemberDetails(memberName, "ab123456c", rasDate)

      val json = Json.toJson(memberDetails)

      (json \ "nino").as[String] shouldBe "AB123456C"
      (json \ "name" \ "firstName").as[String] shouldBe "John"
      (json \ "name" \ "lastName").as[String] shouldBe "Smith"
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "name" -> Json.obj(
          "firstName" -> "Jane",
          "lastName" -> "Doe"
        ),
        "nino" -> "CD987654E",
        "dateOfBirth" -> Json.obj(
          "day" -> "20",
          "month" -> "6",
          "year" -> "1985"
        )
      )

      val result = Json.fromJson[MemberDetails](json)

      result shouldBe a[JsSuccess[_]]
      val memberDetails = result.get
      memberDetails.name.firstName shouldBe "Jane"
      memberDetails.name.lastName shouldBe "Doe"
      memberDetails.nino shouldBe "CD987654E"
      memberDetails.dateOfBirth.day shouldBe Some("20")
      memberDetails.dateOfBirth.month shouldBe Some("6")
      memberDetails.dateOfBirth.year shouldBe Some("1985")
    }
  }
}