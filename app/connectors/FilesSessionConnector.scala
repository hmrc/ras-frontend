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

package connectors

import com.google.inject.Singleton
import config.ApplicationConfig
import models.{CreateFileSessionRequest, FileSession}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FilesSessionConnector @Inject()(http: HttpClientV2,
                                     appConfig: ApplicationConfig) extends Logging {

  lazy val serviceUrl: String = appConfig.rasApiBaseUrl

  def createFileSession(request: CreateFileSessionRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val fullURL = s"$serviceUrl/create-file-session"
    http
      .post(url"$fullURL")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map {
        case response if response.status == CREATED => true
        case _ => false
      }
  }

  def fetchFileSession(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FileSession]] = {
    val fullURL = s"$serviceUrl/get-file-session/$userId"
    http
      .get(url"$fullURL")
      .execute[HttpResponse]
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
  }

  def deleteFileSession(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val fullURL = s"$serviceUrl/delete-file-session/$userId"
    http
      .delete(url"$fullURL")
      .execute[HttpResponse]
      .map {
        case response if response.status == NO_CONTENT => true
        case _ => false
      }
  }
}
