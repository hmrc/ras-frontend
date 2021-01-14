/*
 * Copyright 2021 HM Revenue & Customs
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

import java.io.InputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import config.ApplicationConfig
import javax.inject.Inject
import models.{ApiVersion, MemberDetails, ResidencyStatus}
import play.api.Logger
import play.api.libs.json.{JsSuccess, JsValue}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class ResidencyStatusAPIConnector @Inject()(val http: DefaultHttpClient,
																						val appConfig: ApplicationConfig) {

  lazy val serviceUrl: String = appConfig.rasApiBaseUrl
  lazy val residencyStatusUrl: String = appConfig.rasApiResidencyStatusEndpoint
  lazy val fileDeletionUrl: String = appConfig.fileDeletionUrl
  lazy val residencyStatusVersion: ApiVersion = appConfig.rasApiVersion

  def getResidencyStatus(memberDetails: MemberDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ResidencyStatus] = {

    val rasUri = s"$serviceUrl/$residencyStatusUrl"

    val headerCarrier = hc.withExtraHeaders("Accept" -> s"application/vnd.hmrc.$residencyStatusVersion+json", "Content-Type" -> "application/json" )

    Logger.info(s"[ResidencyStatusAPIConnector][getResidencyStatus] Calling Residency Status api")

    http.POST[JsValue, ResidencyStatus](rasUri, memberDetails.asCustomerDetailsPayload)(implicitly, rds = responseHandler, headerCarrier, ec)
  }

  def getFile(fileName: String, userId: String)(implicit hc: HeaderCarrier): Future[Option[InputStream]] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    Logger.info(s"[ResidencyStatusAPIConnector][getFile] Get results file with URI for $fileName by userId ($userId)")
    http.buildRequest(s"$serviceUrl/ras-api/file/getFile/$fileName").stream().map { res =>
      Some(res.bodyAsSource.runWith(StreamConverters.asInputStream()))
    }

  }

  def deleteFile(fileName: String, userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    http.DELETE[HttpResponse](s"$serviceUrl$fileDeletionUrl/$fileName/$userId")(implicitly, hc, ec)
  }

  private val responseHandler = new HttpReads[ResidencyStatus] {
    override def read(method: String, url: String, response: HttpResponse): ResidencyStatus = {
      response.status match {
        case 200 => Try(response.json.validate[ResidencyStatus]) match {
          case Success(JsSuccess(payload, _)) => payload
          case _ => Logger.error("[ResidencyStatusAPIConnector][responseHandler] There was a problem parsing the response json.")
                    throw new InternalServerException("ResidencyStatusAPIConnector responseHandler | Response json could not be parsed.")
        }
        case 400 => Logger.error("[ResidencyStatusAPIConnector][responseHandler] Data sent to the API was not sent in the correct format.")
                    throw new InternalServerException("Internal Server Error")
        case 403 => Logger.info("[ResidencyStatusAPIConnector][responseHandler] Member not found.")
                    throw Upstream4xxResponse("Member not found", 403, 403, response.allHeaders)
        case _ => Logger.error(s"[ResidencyStatusAPIConnector][responseHandler] ${response.status} status code received from RAS-API.")
                  throw new InternalServerException("Internal Server Error")
      }
    }
  }
}
