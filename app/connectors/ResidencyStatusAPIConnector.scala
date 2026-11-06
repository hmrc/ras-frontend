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
import models.FailureReason.*
import models.{ConnectorFailure, MemberDetails, ResidencyStatus}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, StreamConverters}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.{HttpClientV2, readStreamHttpResponse}
import utils.LogHelper.{failureText, successText}

import java.io.InputStream
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ResidencyStatusAPIConnector @Inject() (
  http: HttpClientV2,
  appConfig: ApplicationConfig,
  mat: Materializer
) extends Logging {

  lazy val serviceUrl: String         = appConfig.rasApiBaseUrl
  lazy val residencyStatusUrl: String = appConfig.rasApiResidencyStatusEndpoint
  lazy val fileDeletionUrl: String    = appConfig.fileDeletionUrl

  private val getResidencyStatusTag = "[ResidencyStatusAPIConnector][getResidencyStatus]"

  def getResidencyStatus(
    memberDetails: MemberDetails,
    userId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ResidencyStatus] = {
    val fullUrl       = s"$serviceUrl/$residencyStatusUrl"
    val headerCarrier = hc.withExtraHeaders(
      "Accept"       -> s"application/vnd.hmrc.2.0+json",
      "Content-Type" -> "application/json"
    )

    logger.info(s"$getResidencyStatusTag Calling Residency Status api userId=$userId")

    http
      .post(url"$fullUrl")(headerCarrier)
      .withBody(memberDetails.asCustomerDetailsPayload)
      .execute[HttpResponse]
      .recover(networkFailure(getResidencyStatusTag, userId))
      .map(response => toResidencyStatus(response, userId))
  }

  def getFile(fileName: String, userId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[InputStream]] = {

    val tag = "[ResidencyStatusAPIConnector][getFile]"

    val requiredHeaders: Seq[(String, String)] = hc.headers(HeaderNames.explicitlyIncludedHeaders)
    val fullUrl                                = s"$serviceUrl/ras-api/file/getFile/$fileName"

    logger.info(s"$tag Calling RAS-API for results file userId=$userId fileName=$fileName")

    http
      .get(url"$fullUrl")
      .transform(
        requiredHeaders
          .foldLeft(_)((request: WSRequest, headers: (String, String)) => request.addHttpHeaders(headers))
      )
      .stream[HttpResponse]
      .recover(networkFailure(tag, userId))
      .map { response =>
        response.status match {
          case 200    =>
            logger.info(successText(tag, userId, s"status=200 fileName=$fileName"))
            Some(response.bodyAsSource.runWith(StreamConverters.asInputStream())(using mat))
          case status =>

            // consume the body, as with a streamed response it can keep the underlying connection open until it times out
            response.bodyAsSource.runWith(Sink.ignore)(using mat)

            val failure = ConnectorFailure(UNEXPECTED_STATUS, Some(status))
            logger.error(failureText(tag, userId, failure, s"Results file could not be retrieved fileName=$fileName"))
            throw failure
        }
      }
  }

  def deleteFile(fileName: String, userId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[HttpResponse] = {
    val fullURL = s"$serviceUrl$fileDeletionUrl$fileName/$userId"

    val tag = "[ResidencyStatusAPIConnector][deleteFile]"

    http
      .delete(url"$fullURL")
      .execute[HttpResponse]
      .recover(networkFailure(tag, userId))
      .map { response =>
        if (response.status >= 200 && response.status < 300) {
          logger.info(successText(tag, userId, s"status=${response.status} fileName=$fileName"))
        } else {
          val failure = ConnectorFailure(UNEXPECTED_STATUS, Some(response.status))
          logger.warn(failureText(tag, userId, failure, s"Results file was not deleted fileName=$fileName"))
        }
        response
      }
  }

  private def toResidencyStatus(response: HttpResponse, userId: String)(implicit hc: HeaderCarrier): ResidencyStatus = {
    val tag    = getResidencyStatusTag
    val status = Some(response.status)

    response.status match {
      case 200 =>
        Try(response.json.validate[ResidencyStatus]) match {
          case Success(JsSuccess(payload, _)) =>
            logger.info(successText(tag, userId, "status=200"))
            payload

          case Success(JsError(errors)) =>
            val failure = ConnectorFailure.invalidResponse(response.status, errors)
            logger.error(failureText(tag, userId, failure, "Response json did not match expected format"))
            throw failure

          case Failure(exception) =>
            val failure = ConnectorFailure.parseError(response.status, exception)
            logger.error(failureText(tag, userId, failure, "Response body was not valid json"))
            throw failure
        }

      case 400 =>
        val failure = ConnectorFailure(BAD_REQUEST, status)
        logger.error(failureText(tag, userId, failure, "Data sent to the API was not in the correct format"))
        throw failure

      case 403 =>
        val failure = ConnectorFailure(MATCHING_FAILED, status)
        logger.info(failureText(tag, userId, failure, "Member not found"))
        throw failure

      case 429 =>
        val failure = ConnectorFailure(TOO_MANY_REQUESTS, status)
        logger.warn(failureText(tag, userId, failure))
        throw failure

      case 500 =>
        val failure = ConnectorFailure(INTERNAL_SERVER_ERROR, status)
        logger.error(failureText(tag, userId, failure, "RAS-API could not provide a residency status"))
        throw failure

      case 503 =>
        val failure = ConnectorFailure(SERVICE_UNAVAILABLE, status)
        logger.warn(failureText(tag, userId, failure))
        throw failure

      case _ =>
        val failure = ConnectorFailure(UNEXPECTED_STATUS, status)
        logger.error(failureText(tag, userId, failure, "Unexpected status code received from RAS-API"))
        throw failure
    }
  }

  private def networkFailure(tag: String, userId: String)(implicit
    hc: HeaderCarrier
  ): PartialFunction[Throwable, Nothing] = {
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
