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

package services

import java.util.UUID

import connectors.FileUploadConnector
import play.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UploadService extends ServicesConfig {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val rasFrontendBaseUrl = baseUrl("ras-frontend")
  lazy val rasFrontendUrlSuffix = getString("ras-frontend-url-suffix")
  lazy val fileUploadFrontendBaseUrl = baseUrl("file-upload-frontend")
  lazy val fileUploadFrontendSuffix = getString("file-upload-frontend-url-suffix")

  val fileUploadConnector: FileUploadConnector

  def createFileUploadUrl(): Future[Option[String]] = {
    val envelopeIdPattern = "envelopes/([\\w\\d-]+)$".r.unanchored
    fileUploadConnector.createEnvelope().map { response =>
      response.header("Location") match {
        case Some(locationHeader) =>
          locationHeader match {
            case envelopeIdPattern(id) =>
              Logger.debug("[UploadService][createFileUploadUrl] Envelope id obtained")

              val fileUploadUrl = s"$fileUploadFrontendBaseUrl/$fileUploadFrontendSuffix/$id/files/${UUID.randomUUID().toString}"
              val successRedirectUrl = s"redirect-success-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/upload-success"
              val errorRedirectUrl = s"redirect-error-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/upload-error"
              val completeFileUploadUrl = s"${fileUploadUrl}?${successRedirectUrl}&${errorRedirectUrl}"

              Some(completeFileUploadUrl)
            case _ =>
              Logger.debug("[UploadService][createFileUploadUrl] Failed to obtain an envelope id from location header")
              None
          }
        case _ =>
          Logger.debug("[UploadService][createFileUploadUrl] Failed to find a location header in the response")
          None
      }
    }
  }
}

object UploadService extends UploadService {
  // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
  override val fileUploadConnector = FileUploadConnector
  // $COVERAGE-ON$
}
