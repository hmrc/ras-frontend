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

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class UploadDetails(uploadTimestamp: Instant, checksum: String, fileMimeType: String, fileName: String, size: Int)

case class FailureDetails(failureReason: String, message: String)

object FailureDetails {
  given formats: OFormat[FailureDetails] = Json.format[FailureDetails]
}

object UploadDetails {
  val empty: UploadDetails              = UploadDetails(Instant.now(), "", "", "", 0)
  given formats: OFormat[UploadDetails] = Json.format[UploadDetails]
}

case class CallbackData(
  reference: String,
  downloadUrl: Option[String],
  fileStatus: String,
  uploadDetails: Option[UploadDetails],
  failureDetails: Option[FailureDetails]
)

object CallbackData {
  given formats: OFormat[CallbackData] = Json.format[CallbackData]
}

case class ResultsFileMetaData(
  id: String,
  filename: Option[String],
  uploadDate: Option[Long],
  chunkSize: Int,
  length: Long
)

object ResultsFileMetaData {
  given formats: OFormat[ResultsFileMetaData] = Json.format[ResultsFileMetaData]
}

case class FileMetadata(id: String, name: Option[String], created: Option[String])

object FileMetadata {
  given format: OFormat[FileMetadata] = Json.format[FileMetadata]
}

case class FileSession(
  userFile: Option[CallbackData],
  resultsFile: Option[ResultsFileMetaData],
  userId: String,
  uploadTimeStamp: Option[Long],
  fileMetadata: Option[FileMetadata]
)

object FileSession {
  given format: OFormat[FileSession] = Json.format[FileSession]
}
