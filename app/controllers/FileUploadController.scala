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

package controllers

import java.util.UUID

import config.{ApplicationConfig, FrontendAuthConnector, RasContext, RasContextImpl}
import connectors.{FileUploadConnector, UserDetailsConnector}
import models.{Envelope, UploadResponse}
import play.Logger
import play.api.mvc.{Action, Request}
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait FileUploadController extends RasController with PageFlowController {

  implicit val context: RasContext = RasContextImpl
  val fileUploadConnector: FileUploadConnector

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession().flatMap {
            case Some(session) =>
              if(session.aFileIsInProcess.getOrElse(false)){
                Logger.debug("[FileUploadController][get] a file is still processing")
                Future.successful(Redirect(routes.DashboardController.get))
              }else{
                createFileUploadUrl(session.envelope)(request, hc).flatMap {
                  case Some(url) =>
                    Logger.debug("[FileUploadController][get] form url created successfully")
                    val error = extractErrorReason(session.uploadResponse)
                    Future.successful(Ok(views.html.file_upload(url,error)))
                  case _ =>
                    Logger.debug("[FileUploadController][get] failed to obtain a form url using existing envelope")
                    Future.successful(Redirect(routes.GlobalErrorController.get))
                }
              }
            case _ =>
              createFileUploadUrl(None)(request, hc).flatMap {
                case Some(url) =>
                  Logger.debug("[FileUploadController][get] stored new envelope id successfully")
                  Future.successful(Ok(views.html.file_upload(url,"")))
                case _ =>
                  Logger.debug("[FileUploadController][get] failed to obtain a form url using new envelope")
                  Future.successful(Redirect(routes.GlobalErrorController.get))
              }
          }.recover {
            case e: Throwable =>
              Logger.error("[FileUploadController][get] failed to fetch ras session")
              Redirect(routes.GlobalErrorController.get)
          }
        case Left(resp) =>
          Logger.debug("[FileUploadController][get] user not authorised")
          resp
      }
  }

  def createFileUploadUrl(envelope: Option[Envelope])(implicit request: Request[_], hc:HeaderCarrier): Future[Option[String]] = {

    val config = ApplicationConfig
    val rasFrontendBaseUrl = config.getString("ras-frontend.host")
    val rasFrontendUrlSuffix = config.getString("ras-frontend-url-suffix")
    val fileUploadFrontendBaseUrl = config.getString("file-upload-frontend.host")
    val fileUploadFrontendSuffix = config.getString("file-upload-frontend-url-suffix")
    val envelopeIdPattern = "envelopes/([\\w\\d-]+)$".r.unanchored
    val successRedirectUrl = s"redirect-success-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/upload-success"
    val errorRedirectUrl = s"redirect-error-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/upload-error"

    envelope match {
      case Some(envelope) =>
        val fileUploadUrl = s"$fileUploadFrontendBaseUrl/$fileUploadFrontendSuffix/${envelope.id}/files/${UUID.randomUUID().toString}"
        val completeFileUploadUrl = s"${fileUploadUrl}?${successRedirectUrl}&${errorRedirectUrl}"
        Future.successful(Some(completeFileUploadUrl))
      case _ =>
        fileUploadConnector.createEnvelope().flatMap { response =>
          response.header("Location") match {
            case Some(locationHeader) =>
              locationHeader match {
                case envelopeIdPattern(id) =>
                  sessionService.cacheEnvelope(Envelope(id)).map {
                    case Some(session) =>
                      Logger.debug("[UploadService][createFileUploadUrl] Envelope id obtained and cached")
                      val fileUploadUrl = s"$fileUploadFrontendBaseUrl/$fileUploadFrontendSuffix/$id/files/${UUID.randomUUID().toString}"
                      val completeFileUploadUrl = s"${fileUploadUrl}?${successRedirectUrl}&${errorRedirectUrl}"
                      Some(completeFileUploadUrl)
                    case _ =>
                      Logger.debug("[FileUploadController][get] failed to retrieve cache after storing the envelope")
                      None
                  }
                case _ =>
                  Logger.debug("[UploadService][createFileUploadUrl] Failed to obtain an envelope id from location header")
                  Future.successful(None)
              }
            case _ =>
              Logger.debug("[UploadService][createFileUploadUrl] Failed to find a location header in the response")
              Future.successful(None)
          }
        }.recover {
          case e: Throwable =>
            Logger.debug("[UploadService][createFileUploadUrl] Failed to create envelope")
            None
        }
    }
  }

  def back = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userInfo) => Future.successful(previousPage("FileUploadController"))
        case Left(res) => res
      }
  }

  def uploadSuccess = Action.async { implicit request =>
    isAuthorised.flatMap {
      case Right(_) =>
        sessionService.cacheFileInProcess(true).map{
          case Some(session) =>
            Logger.debug("[FileUploadController][uploadSuccess] upload has been successful")
            Ok(views.html.file_upload_successful())
          case _ =>
            Redirect(routes.GlobalErrorController.get())
        }
      case Left(resp) =>
        Logger.debug("[FileUploadController][uploadSuccess] user not authorised")
        resp
    }
  }

  def uploadError = Action.async { implicit request =>
    isAuthorised.flatMap {
      case Right(_) =>
        val errorCode = request.getQueryString("errorCode").getOrElse("")
        val errorReason = request.getQueryString("reason").getOrElse("")
        val errorResponse = UploadResponse(errorCode, Some(errorReason))
        sessionService.cacheUploadResponse(errorResponse).flatMap {
          case Some(session) => Future.successful(Redirect(routes.FileUploadController.get()))
          case _ => Future.successful(Redirect(routes.GlobalErrorController.get()))
        }
      case Left(resp) =>
        Logger.debug("[FileUploadController][uploadError] user not authorised")
        resp
    }
  }

  private def extractErrorReason(uploadResponse: Option[UploadResponse]):String ={
    uploadResponse match {
      case Some(response) =>
        response.code match {
          case "400" if response.reason.getOrElse("").contains(Messages("file.upload.empty.file.reason")) =>
            Logger.debug("[FileUploadController][extractErrorReason] empty file")
            Messages("file.empty.error")
          case "400" =>
            Logger.debug("[FileUploadController][extractErrorReason] bad request")
            Messages("upload.failed.error")
          case "404" =>
            Logger.debug("[FileUploadController][extractErrorReason] enveloper not found")
            Messages("upload.failed.error")
          case "413" =>
            Logger.debug("[FileUploadController][extractErrorReason] file too large")
            Messages("file.large.error")
          case "415" =>
            Logger.debug("[FileUploadController][extractErrorReason] file type other than the supported type")
            Messages("upload.failed.error")
          case "423" =>
            Logger.debug("[FileUploadController][extractErrorReason] routing request has been made for this Envelope. Envelope is locked")
            Messages("upload.failed.error")
          case _ =>
            Logger.debug("[FileUploadController][extractErrorReason] unknown cause")
            Messages("upload.failed.error")
        }
      case _ => ""
    }
  }
}

object FileUploadController extends FileUploadController {
  // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  override val fileUploadConnector = FileUploadConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  // $COVERAGE-ON$
}
