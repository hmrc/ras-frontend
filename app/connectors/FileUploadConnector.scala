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

import config.ApplicationConfig
import javax.inject.Inject
import models.ApiVersion
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import scala.concurrent.{ExecutionContext, Future}

class FileUploadConnector @Inject()(val http: DefaultHttpClient,
																		appConfig: ApplicationConfig) {

  lazy val rasApiBaseUrl: String = appConfig.rasApiBaseUrl
  lazy val fileUploadBaseUrl: String = appConfig.fileUploadBaseUrl
  lazy val fileUploadUrlSuffix: String = appConfig.fileUploadUrlSuffix
  lazy val rasFileUploadCallbackUrl: String = appConfig.fileUploadCallBack

  lazy val maxItems: Int = appConfig.maxItems
  lazy val maxSize: String = appConfig.maxSize
  lazy val maxSizePerItem: String = appConfig.maxSizePerItem
  lazy val allowZeroLengthFiles: Boolean = appConfig.allowZeroLengthFiles
  lazy val apiVersion: ApiVersion = appConfig.rasApiVersion


  def createEnvelope(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val requestBody = Json.parse(
      s"""
        {
          "callbackUrl": "$rasApiBaseUrl$rasFileUploadCallbackUrl/$userId?version=$apiVersion",
          "constraints": 	{
              "maxItems": 1,
              "maxSize": "$maxSize",
              "maxSizePerItem": "$maxSizePerItem",
              "contentTypes": ["text/xml"],
              "allowZeroLengthFiles": $allowZeroLengthFiles
              }
          }
      """.stripMargin)

    http.POST[JsValue, HttpResponse](
      s"$fileUploadBaseUrl/$fileUploadUrlSuffix", requestBody, Seq()
    )

  }
}
