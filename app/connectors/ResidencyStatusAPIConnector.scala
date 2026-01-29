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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.StreamConverters
import config.ApplicationConfig
import models.{ApiVersion, MemberDetails, ResidencyStatus}
import play.api.Logging
import play.api.libs.json.JsSuccess
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import java.io.InputStream
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ResidencyStatusAPIConnector @Inject() (http: HttpClientV2, appConfig: ApplicationConfig) extends Logging {

  lazy val serviceUrl: String                 = appConfig.rasApiBaseUrl
  lazy val residencyStatusUrl: String         = appConfig.rasApiResidencyStatusEndpoint
  lazy val fileDeletionUrl: String            = appConfig.fileDeletionUrl
  lazy val residencyStatusVersion: ApiVersion = appConfig.rasApiVersion

  def getResidencyStatus(
    memberDetails: MemberDetails
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ResidencyStatus] = {
    val fullUrl       = s"$serviceUrl/$residencyStatusUrl"
    val headerCarrier = hc.withExtraHeaders(
      "Accept"       -> s"application/vnd.hmrc.$residencyStatusVersion+json",
      "Content-Type" -> "application/json"
    )
    logger.info(s"[ResidencyStatusAPIConnector][getResidencyStatus] Calling Residency Status api")
    http
      .post(url"$fullUrl")(headerCarrier)
      .withBody(memberDetails.asCustomerDetailsPayload)
      .execute[HttpResponse]
      .map(toResidencyStatus)
  }

  def getFile(fileName: String, userId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[InputStream]] = {
    implicit val system: ActorSystem           = ActorSystem()
    val requiredHeaders: Seq[(String, String)] = hc.headers(HeaderNames.explicitlyIncludedHeaders)
    val fullUrl                                = s"$serviceUrl/ras-api/file/getFile/$fileName"
    logger.info(s"[ResidencyStatusAPIConnector][getFile] Get results file with URI for $fileName by userId ($userId)")
    http
      .get(url"$fullUrl")
      .transform(
        requiredHeaders
          .foldLeft(_)((request: WSRequest, headers: (String, String)) => request.addHttpHeaders(headers))
      )
      .stream[HttpResponse]
      .map { res: HttpResponse =>
        Some(res.bodyAsSource.runWith(StreamConverters.asInputStream()))
      }
  }

  def deleteFile(fileName: String, userId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[HttpResponse] = {
    val fullURL = s"$serviceUrl$fileDeletionUrl$fileName/$userId"
    http
      .delete(url"$fullURL")
      .execute[HttpResponse]
  }

  private def toResidencyStatus(response: HttpResponse): ResidencyStatus =
    response.status match {
      case 200 =>
        Try(response.json.validate[ResidencyStatus]) match {
          case Success(JsSuccess(payload: ResidencyStatus, _)) => payload
          case _                                               =>
            logger.error(
              "[ResidencyStatusAPIConnector][responseHandler] There was a problem parsing the response json."
            )
            throw new InternalServerException(
              "ResidencyStatusAPIConnector responseHandler | Response json could not be parsed."
            )
        }
      case 400 =>
        logger.error(
          "[ResidencyStatusAPIConnector][responseHandler] Data sent to the API was not sent in the correct format."
        )
        throw new InternalServerException("Internal Server Error")
      case 403 =>
        logger.info("[ResidencyStatusAPIConnector][responseHandler] Member not found.")
        throw UpstreamErrorResponse("Member not found", 403, 403, response.headers)
      case _   =>
        logger.error(
          s"[ResidencyStatusAPIConnector][responseHandler] ${response.status} status code received from RAS-API."
        )
        throw new InternalServerException("Internal Server Error")
    }

}
