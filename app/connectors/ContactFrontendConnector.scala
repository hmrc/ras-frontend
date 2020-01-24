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

package connectors

import config.{ApplicationConfig, WSHttp}
import play.api.Mode.Mode
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpGet}

trait ContactFrontendConnector extends ServicesConfig {

  import scala.concurrent.ExecutionContext.Implicits.global

  val http: HttpGet = WSHttp

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration

  def getHelpPartial(implicit hc: HeaderCarrier): Future[String] = {

    val url = ApplicationConfig.reportAProblemUrl

    http.GET(url) map { r =>
      r.body
    } recover {
      case e: BadGatewayException =>
        Logger.error(s"[ContactFrontendConnector] ${e.message}", e)
        ""
    }
  }

}

object ContactFrontendConnector extends ContactFrontendConnector