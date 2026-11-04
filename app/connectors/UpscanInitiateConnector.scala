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

import config.ApplicationConfig
import models.ConnectorFailure
import models.FailureReason.*
import models.upscan.{PreparedUpload, UpscanFileReference, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{BadGatewayException, GatewayTimeoutException, HeaderCarrier, HttpResponse, StringContextOps}
import utils.LogHelper.{failureText, successText}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class UpscanInitiateConnector @Inject() (httpClient: HttpClientV2, appConfig: ApplicationConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  private val headers: (String, String)             = (HeaderNames.CONTENT_TYPE, "application/json")
  private val upscanInitiateUrl: URL                = url"${appConfig.initiateUrl}"
  private lazy val rasFileUploadCallbackUrl: String = appConfig.upscanCallbackEndpoint
  lazy val rasApiBaseUrl: String                    = appConfig.rasApiBaseUrl

  private val tag = "[UpscanInitiateConnector][initiateUpscan]"

  def initiateUpscan(userId: String, redirectOnSuccess: Option[String], redirectOnError: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[UpscanInitiateResponse] = {
    val request: UpscanInitiateRequest = UpscanInitiateRequest(
      callbackUrl = s"$rasApiBaseUrl$rasFileUploadCallbackUrl/$userId",
      successRedirect = redirectOnSuccess,
      errorRedirect = redirectOnError,
      maximumFileSize = Some(appConfig.maxFileSize)
    )

    logger.info(s"$tag Calling upscan-initiate userId=$userId")

    httpClient
      .post(upscanInitiateUrl)(hc)
      .withBody(Json.toJson(request))
      .setHeader(headers)
      .execute[HttpResponse]
      .recover(networkFailure(userId))
      .map(response => toUpscanInitiateResponse(response, userId))
  }

  private def toUpscanInitiateResponse(response: HttpResponse, userId: String)(implicit
    hc: HeaderCarrier
  ): UpscanInitiateResponse =
    response.status match {
      case OK =>
        Try(response.json.validate[PreparedUpload]) match {
          case Success(JsSuccess(preparedUpload, _)) =>

            logger.info(successText(tag, userId, s"status=$OK reference=${preparedUpload.reference.value}"))

            UpscanInitiateResponse(
              fileReference = UpscanFileReference(preparedUpload.reference.value),
              postTarget = preparedUpload.uploadRequest.href,
              formFields = preparedUpload.uploadRequest.fields
            )

          case Success(JsError(errors)) =>
            val failure = ConnectorFailure.invalidResponse(OK, errors)
            logger.error(failureText(tag, userId, failure, "Upscan initiate response did not match expected format"))
            throw failure

          case Failure(exception) =>
            val failure = ConnectorFailure.parseError(OK, exception)
            logger.error(failureText(tag, userId, failure, "Upscan initiate response was not valid json"))
            throw failure
        }

      case status =>
        val failure = ConnectorFailure(INTERNAL_SERVER_ERROR, status = Some(status))
        logger.error(failureText(tag, userId, failure, "Unexpected status code received from upscan-initiate"))
        throw failure
    }

  private def networkFailure(userId: String)(implicit hc: HeaderCarrier): PartialFunction[Throwable, Nothing] = {
    case e: GatewayTimeoutException =>
      val failure = ConnectorFailure(TIMEOUT, cause = Some(e))
      logger.error(failureText(tag, userId, failure, "Request timed out"))
      throw failure

    case e: BadGatewayException =>
      val failure = ConnectorFailure(CONNECTION_FAILURE, cause = Some(e))
      logger.error(failureText(tag, userId, failure, "Could not connect"))
      throw failure
  }

}
