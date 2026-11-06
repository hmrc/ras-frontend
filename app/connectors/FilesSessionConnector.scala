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
import models.FailureReason.{CONNECTION_FAILURE, TIMEOUT, UNEXPECTED_STATUS}
import models.{ConnectorFailure, CreateFileSessionRequest, FileSession}
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HeaderCarrier, HttpResponse, StringContextOps}
import utils.LogHelper.{failureText, successText}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class FilesSessionConnector @Inject() (http: HttpClientV2, appConfig: ApplicationConfig) extends Logging {

  lazy val serviceUrl: String = appConfig.rasApiBaseUrl

  def createFileSession(
    request: CreateFileSessionRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val fullURL = s"$serviceUrl/create-file-session"
    val tag     = "[FilesSessionConnector][createFileSession]"

    http
      .post(url"$fullURL")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map {
        case response if response.status == CREATED =>
          logger.info(successText(tag, request.userId, s"status=${response.status} reference=${request.reference}"))
          true

        case response =>
          val failure = ConnectorFailure(UNEXPECTED_STATUS, status = Some(response.status))
          logger.error(
            failureText(tag, request.userId, failure, s"File session was not created reference=${request.reference}")
          )
          false
      }
      .recover(networkFailure(tag, request.userId, returnValue = false, log = logger.error(_)))
  }

  def fetchFileSession(
    userId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FileSession]] = {
    val fullURL = s"$serviceUrl/get-file-session/$userId"
    val tag     = "[FilesSessionConnector][fetchFileSession]"

    http
      .get(url"$fullURL")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Try(response.json.validate[FileSession]) match {
              case Success(JsSuccess(fileSession, _)) =>
                Some(fileSession)

              case Success(JsError(errors)) =>
                val failure = ConnectorFailure.invalidResponse(response.status, errors)
                logger.error(failureText(tag, userId, failure, "File session json did not match expected format"))
                None

              case Failure(exception) =>
                val failure = ConnectorFailure.parseError(response.status, exception)
                logger.error(failureText(tag, userId, failure, "File session response was not valid json"))
                None
            }

          case NOT_FOUND =>
            logger.info(s"$tag No file session found userId=$userId status=$NOT_FOUND")
            None

          case status =>
            val failure = ConnectorFailure(UNEXPECTED_STATUS, status = Some(status))
            logger.error(failureText(tag, userId, failure, "Received non-OK status code from API"))
            None
        }
      }
      .recover(networkFailure(tag, userId, returnValue = None, log = logger.error(_)))
  }

  def deleteFileSession(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val fullURL = s"$serviceUrl/delete-file-session/$userId"
    val tag     = "[FilesSessionConnector][deleteFileSession]"

    http
      .delete(url"$fullURL")
      .execute[HttpResponse]
      .map {
        case response if response.status == NO_CONTENT =>
          logger.info(successText(tag, userId, s"status=${response.status}"))
          true

        case response =>
          val failure = ConnectorFailure(UNEXPECTED_STATUS, status = Some(response.status))
          logger.warn(failureText(tag, userId, failure, "File session was not deleted"))
          false
      }
      .recover(networkFailure(tag, userId, returnValue = false, log = logger.warn(_)))
  }

  private def networkFailure[A](tag: String, userId: String, returnValue: A, log: String => Unit)(implicit
    hc: HeaderCarrier
  ): PartialFunction[Throwable, A] = {
    case e: GatewayTimeoutException =>
      log(failureText(tag, userId, ConnectorFailure(TIMEOUT, cause = Some(e)), "Request timed out"))
      returnValue

    case e: BadGatewayException =>
      log(failureText(tag, userId, ConnectorFailure(CONNECTION_FAILURE, cause = Some(e)), "Could not connect"))
      returnValue
  }

}
