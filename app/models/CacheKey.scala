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

trait CacheKey[+T]

object CacheKey {
  case object Name extends CacheKey[MemberName]
  case object Nino extends CacheKey[MemberNino]
  case object Dob extends CacheKey[MemberDateOfBirth]
  case object StatusResult extends CacheKey[Option[ResidencyStatusResult]]
  case object UploadResponse extends CacheKey[Option[UploadResponse]]
  case object File extends CacheKey[Option[File]]
  case object All extends CacheKey[RasSession]
}
