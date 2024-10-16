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

import config.ApplicationConfig
import models.ApiVersion
import models.upscan.{PreparedUpload, UpscanFileReference, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.libs.json.{Json, OFormat}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpscanInitiateConnector @Inject()(httpClient: HttpClientV2, appConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  private val headers: (String, String) = (HeaderNames.CONTENT_TYPE, "application/json")

  private val upscanInitiateUrl: URL = url"${appConfig.initiateUrl}"
  lazy val rasApiBaseUrl: String = appConfig.rasApiBaseUrl
  lazy val rasFileUploadCallbackUrl: String = appConfig.upscanCallbackEndpoint
  lazy val apiVersion: ApiVersion = appConfig.rasApiVersion


  def initiateUpscan(userId: String, redirectOnSuccess: Option[String], redirectOnError: Option[String])
                (implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] = {
    val request: UpscanInitiateRequest = UpscanInitiateRequest(
      callbackUrl = s"$rasApiBaseUrl$rasFileUploadCallbackUrl/$userId?version=$apiVersion",
      successRedirect = redirectOnSuccess,
      errorRedirect = redirectOnError,
      maximumFileSize = Some(appConfig.maxFileSize)
    )

    initiate(upscanInitiateUrl, request)
  }

  private def initiate(url: URL, request: UpscanInitiateRequest)(
    implicit hc: HeaderCarrier,
    format: OFormat[UpscanInitiateRequest]): Future[UpscanInitiateResponse] =
    for {
      response: PreparedUpload <- httpClient
        .post(url)(hc)
        .withBody(Json.toJson(request))
        .setHeader(headers)
        .execute[PreparedUpload]
      fileReference = UpscanFileReference(response.reference.value)
      postTarget = response.uploadRequest.href
      formFields = response.uploadRequest.fields
    } yield UpscanInitiateResponse(fileReference, postTarget, formFields)
}
