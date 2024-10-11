/*
 * Copyright 2023 HM Revenue & Customs
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

package models.upscan

import play.api.libs.json._

sealed trait UploadStatus

case object InProgress extends UploadStatus

case object Failed extends UploadStatus

case object NotStarted extends UploadStatus

case class UploadedSuccessfully(name: String, mimeType: String, downloadUrl: String, size: Option[Long]) extends UploadStatus

object UploadedSuccessfully {
  implicit val uploadedSuccessfullyFormat: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]
}

object UploadStatus {
  implicit val readsUploadStatus: Reads[UploadStatus] = new Reads[UploadStatus] {
    override def reads(json: JsValue): JsResult[UploadStatus] = {
      json match {
        case JsString(value: String) => value match {
          case "NotStarted" => JsSuccess(NotStarted)
          case "InProgress" => JsSuccess(InProgress)
          case "Failed" => JsSuccess(Failed)
          case "UploadedSuccessfully" => Json.fromJson[UploadedSuccessfully](json)(UploadedSuccessfully.uploadedSuccessfullyFormat)
          case _ => JsError(s"Unexpected value of _type: $value")
        }
        case _ => JsError("Missing _type field")
      }
    }
  }

  implicit val writesUploadStatus: Writes[UploadStatus] = {
    case NotStarted => JsString("NotStarted")
    case InProgress => JsString("InProgress")
    case Failed => JsString("Failed")
    case s: UploadedSuccessfully => Json.toJson(s)(UploadedSuccessfully.uploadedSuccessfullyFormat).as[JsObject] + ("_type" -> JsString("UploadedSuccessfully"))
  }
}