/*
 * Copyright 2018 HM Revenue & Customs
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

package connectors

import config.WSHttp
import models._
import play.api.Logger
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.http._

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

trait CustomerMatchingAPIConnector extends ServicesConfig{

  val http: HttpPost = WSHttp

  lazy val serviceUrl = baseUrl("customer-matching")
  lazy val environmentSuffix = getString("customer-matching-environment-suffix")
  lazy val linksSuffix = getString("customer-matching-links-suffix")

  /**
    * Lookup a tax payers details and if matched return a UUID.
    * @param memberDetails
    * @param hc
    * @return a UUID
    */
  def findMemberDetails(memberDetails: MemberDetails)(implicit hc: HeaderCarrier): Future[Option[String]] = {

    val matchingUri = s"$serviceUrl/$environmentSuffix"

    Logger.debug(s"[CustomerMatchingAPIConnector][findMemberDetails] Calling Customer Matching api at ${matchingUri}")

    http.POST[JsValue, Option[String]](matchingUri, memberDetails.asCustomerDetailsPayload, Seq("Accept" -> "application/vnd.hmrc.1.0+json", "Content-Type" -> "application/json" ))(implicitly, rds = responseHandler, hc, MdcLoggingExecutionContext.fromLoggingDetails(hc))
  }

  private val responseHandler = new HttpReads[Option[String]] {
    override def read(method: String, url: String, response: HttpResponse): Option[String] = {
      response.status match {
        case 303 => response.header("Location").map{locationHeader =>
          "[0-9A-Fa-f]{8}(-[0-9A-Fa-f]{4}){3}-[0-9A-Fa-f]{12}".r.findFirstIn(locationHeader)}.getOrElse(None)
        case 403 => throw new Upstream4xxResponse("Member not found", 403, 500, response.allHeaders)
        case _ => None
      }
    }
  }

}

object CustomerMatchingAPIConnector extends CustomerMatchingAPIConnector
