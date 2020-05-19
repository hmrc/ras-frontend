/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{Format, Json}
import play.api.mvc.MultipartFormData.{BadPart, FilePart}

case class MultipartFormData[TemporaryFile](dataParts: Map[String, Seq[String]], files: Seq[FilePart[TemporaryFile]], badParts: Seq[BadPart])

object MultipartFormData {
  implicit val formats: Format[MultipartFormData[TemporaryFile]] = Json.format[MultipartFormData[TemporaryFile]]
}
