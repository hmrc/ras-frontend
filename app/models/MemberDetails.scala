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


import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.format.DateTimeFormatter
import java.util.Locale

case class MemberDetails(name: MemberName,
                         nino: String,
                         dateOfBirth: RasDate) {

  def asCustomerDetailsPayload: JsValue = {
    Json.parse(
      s"""{
          "nino":"${nino.toUpperCase}",
          "firstName":"${name.firstName.capitalize}",
          "lastName":"${name.lastName.capitalize}",
          "dateOfBirth":"${dateOfBirth.asLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.UK))}"
        }
      """)
  }
}

object MemberDetails {
  implicit val memberDetailsReads: Reads[MemberDetails] = (
      (JsPath \ "name").read[MemberName]and
      (JsPath \ "nino").read[String] and
      (JsPath \ "dateOfBirth").read[RasDate]
    )(MemberDetails.apply _)

  implicit val memberDetailsWrites: Writes[MemberDetails] = (
    (JsPath \ "name").write[MemberName] and
      (JsPath \ "nino").write[String].contramap[String](nino => nino.toUpperCase) and
      (JsPath \ "dateOfBirth").write[RasDate]
    )(unlift(MemberDetails.unapply))
}
