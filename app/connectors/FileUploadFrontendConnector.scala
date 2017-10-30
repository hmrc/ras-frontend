/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

import scala.concurrent.Future

trait FileUploadFrontendConnector extends ServicesConfig {

  val httpPost: HttpPost = WSHttp
  lazy val serviceUrl = baseUrl("file-upload-frontend")
  lazy val serviceUrlSuffix = getString("file-upload-frontend-upload")

  def uploadFile(file: String, envelopeId: String, fileId: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val fileUploadUri = s"$serviceUrl/$serviceUrlSuffix/$envelopeId/files/$fileId"
    val requestBody = file

    httpPost.POST[String, Option[String]](fileUploadUri, requestBody,Seq())(implicitly, rds = responseHandler, hc, MdcLoggingExecutionContext.fromLoggingDetails(hc))
  }

  private val responseHandler = new HttpReads[Option[String]] {
    override def read(method: String, url: String, response: HttpResponse): Option[String] = {
      response.status match {
        case 200 => Some("200")
        case _ => None
      }
    }

  }
}


object FileUploadFrontendConnector extends FileUploadFrontendConnector