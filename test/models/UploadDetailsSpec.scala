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
import play.api.libs.json.{JsSuccess, Json}

import java.time.Instant

class UploadDetailsSpec extends AnyWordSpec with Matchers {

  "UploadDetails" should {

    "have an empty instance" in {
      val empty = UploadDetails.empty

      empty.checksum shouldBe ""
      empty.fileMimeType shouldBe ""
      empty.fileName shouldBe ""
      empty.size shouldBe 0
      empty.uploadTimestamp should not be null
    }

    "serialize to JSON" in {
      val uploadDetails = UploadDetails(
        uploadTimestamp = Instant.parse("2023-01-15T10:30:00Z"),
        checksum = "abc123",
        fileMimeType = "text/csv",
        fileName = "test.csv",
        size = 1024
      )

      val json = Json.toJson(uploadDetails)

      (json \ "uploadTimestamp").as[String] shouldBe "2023-01-15T10:30:00Z"
      (json \ "checksum").as[String] shouldBe "abc123"
      (json \ "fileMimeType").as[String] shouldBe "text/csv"
      (json \ "fileName").as[String] shouldBe "test.csv"
      (json \ "size").as[Int] shouldBe 1024
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "uploadTimestamp" -> "2023-01-15T10:30:00Z",
        "checksum" -> "abc123",
        "fileMimeType" -> "text/csv",
        "fileName" -> "test.csv",
        "size" -> 1024
      )

      val result = Json.fromJson[UploadDetails](json)

      result shouldBe a[JsSuccess[_]]
      val uploadDetails = result.get
      uploadDetails.uploadTimestamp shouldBe Instant.parse("2023-01-15T10:30:00Z")
      uploadDetails.checksum shouldBe "abc123"
      uploadDetails.fileMimeType shouldBe "text/csv"
      uploadDetails.fileName shouldBe "test.csv"
      uploadDetails.size shouldBe 1024
    }
  }
}