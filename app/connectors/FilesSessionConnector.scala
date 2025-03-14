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

package connectors

import com.google.inject.Singleton
import config.ApplicationConfig
import models.{CreateFileSessionRequest, FileSession}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.http.HttpClientV2Provider
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FilesSessionConnector @Inject()(val http: HttpClientV2Provider,
                                     val appConfig: ApplicationConfig) extends Logging {

  lazy val serviceUrl: String = appConfig.rasApiBaseUrl

  def createFileSession(request: CreateFileSessionRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    http
      .get()
      .post(url"$serviceUrl/create-file-session")
      .withBody(Json.toJson(request))
      .execute
      .map {
        case response if response.status == CREATED => true
        case _ => false
      }

  def fetchFileSession(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FileSession]] =
    http
      .get()
      .get(url"$serviceUrl/get-file-session/$userId")
      .execute
      .flatMap {
        response =>
          response.status match {
            case OK =>
              Json.parse(response.body).validate[FileSession] match {
                case JsSuccess(value, _) => Future.successful(Some(value))
                case _ => Future.successful(None)
              }
            case status =>
              logger.warn(s"[FilesSessionConnector][fetchFileSession] Received non-OK status code from API: $status")
              Future.successful(None)
          }
      }

  def deleteFileSession(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    http
      .get()
      .delete(url"$serviceUrl/delete-file-session/$userId")
      .execute
      .map {
        case response if response.status == NO_CONTENT => true
        case _ => false
      }
}
