/*
 * Copyright 2020 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.FileUploadConnector
import forms.FileUploadForm.form
import javax.inject.Inject
import models.{Envelope, UploadResponse}
import play.Logger
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, AnyContent, Request, MultipartFormData}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.UnanchoredRegex

class FileUploadController @Inject()(fileUploadConnector: FileUploadConnector,
                                     val authConnector: DefaultAuthConnector,
                                     val shortLivedCache: ShortLivedCache,
                                     val sessionService: SessionService,
                                     val http: DefaultHttpClient,
                                     implicit val appConfig: ApplicationConfig
																		) extends PageFlowController {

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          sessionService.fetchRasSession().flatMap {
            case Some(_) =>
              shortLivedCache.isFileInProgress(userId).flatMap {
                case true =>
                  Logger.info(s"[FileUploadController][get] a file is still processing for userId ($userId) " +
                    s"so another could not be uploaded")
                  Future.successful(Redirect(routes.FileUploadController.uploadInProgress()))
                case _ =>
                  Future.successful(Ok(views.html.file_upload(form, "")))
              }
            case _ =>
              //Temp change to allow page access locally for testing:
              //Logger.error(s"[FileUploadController][get] failed to obtain a session for userId ($userId)")
              //Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
              Future.successful(Ok(views.html.file_upload(form, "")))
          }.recover {
            case e: Throwable =>
              Logger.error(s"[FileUploadController][get] failed to fetch ras session for userId ($userId) - $e")
              Redirect(routes.ErrorController.renderGlobalErrorPage())
          }
        case Left(resp) =>
          Logger.warn("[FileUploadController][get] user not authorised")
          resp
      }
  }

  def post: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised.flatMap {
      case Right(userId) =>
        getFile(request) match {
          case file if (file.filename.length == 0) =>
            Logger.info(s"[FileUploadController][post] No file selected")
            Future.successful(BadRequest(views.html.file_upload(form, Messages("error.select.csv"))))
          case file if (!file.filename.endsWith(".csv")) =>
            Logger.info(s"[FileUploadController][post] Chosen file not a csv")
            Future.successful(BadRequest(views.html.file_upload(form, Messages("error.not.csv"))))
          case _ if (request.headers("Content-Length").toInt > 2097152) =>
            Logger.info(s"[FileUploadController][post] CSV must be smaller than 2MB")
            Future.successful(EntityTooLarge(views.html.file_upload(form, Messages("file.large.error"))))
          case _ =>
            sessionService.fetchRasSession().flatMap {
              case Some(session) =>
                createFileUploadUrl(session.envelope, userId)(request, hc).flatMap {
                  case Some(url) =>
                    Logger.info(s"[FileUploadController][post] form url created successfully for userId ($userId)")
                    val error = extractErrorReason(session.uploadResponse)
                    if (error == Messages("upload.failed.error")) {
                      sessionService.cacheUploadResponse(UploadResponse("", None)).map {
                        case Some(_) =>
                          Redirect(routes.ErrorController.renderProblemUploadingFilePage())
                        case _ =>
                          Logger.error(s"[FileUploadController][post] failed to obtain a session for userId ($userId)")
                          Redirect(routes.ErrorController.renderGlobalErrorPage())
                      }
                    }
                    else {
                      sessionService.resetCacheUploadResponse()
                      Future.successful(Ok(views.html.file_upload(form, error)))
                    }
                  case _ =>
                    Logger.error(s"[FileUploadController][post] failed to obtain a form url using existing envelope " +
                      s"for userId ($userId)")
                    Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
                }
              case _ =>
                createFileUploadUrl(None, userId)(request, hc).flatMap {
                  case Some(url) =>
                    Logger.info(s"[FileUploadController][post] stored new envelope id successfully for userId ($userId)")
                    http.POSTForm(url, request.body.asMultipartFormData.get.dataParts, request.headers.headers).map{ _ =>
                      Redirect(routes.FileUploadController.uploadSuccess())
                    } recover {
                      case err =>
                        Logger.info(s"[FileUploadController][post] file upload failed", err)
                        Redirect(routes.FileUploadController.uploadError())
                    }
                  case _ =>
                    Logger.error(s"[FileUploadController][post] failed to obtain a form url using new envelope for userId ($userId)")
                    Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
                }
            }.recover {
              case e: Throwable =>
                Logger.error(s"[FileUploadController][post] failed to fetch ras session for userId ($userId) - $e")
                Redirect(routes.ErrorController.renderProblemUploadingFilePage())
            }
        }
      case Left(res)
      =>
        Logger.warn("[FileUploadController][post] user Not authorised")
        res
    }
  }

  def getFile(request: Request[AnyContent]): FilePart[Files.TemporaryFile]  = {
    val requestBody = request.body.asMultipartFormData.getOrElse("")
    requestBody match {
      case body:MultipartFormData[Files.TemporaryFile] => body.file("file").get
      case _ => FilePart("","",Some("text"), TemporaryFile(""))
    }
  }

  def createFileUploadUrl(envelope: Option[Envelope], userId: String)(implicit request: Request[_], hc:HeaderCarrier): Future[Option[String]] = {
    lazy val rasFrontendBaseUrl: String = appConfig.rasFrontendBaseUrl
    lazy val rasFrontendUrlSuffix: String = appConfig.rasFrontendUrlSuffix
    lazy val fileUploadFrontendBaseUrl: String = appConfig.fileUploadFrontendBaseUrl
    lazy val fileUploadFrontendSuffix: String = appConfig.fileUploadFrontendSuffix
    val envelopeIdPattern: UnanchoredRegex = "envelopes/([\\w\\d-]+)$".r.unanchored
    val successRedirectUrl: String = s"redirect-success-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/file-uploaded"
    val errorRedirectUrl: String = s"redirect-error-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/file-upload-failed"


    envelope match {
      case Some(envelope) =>
        val fileUploadUrl = s"$fileUploadFrontendBaseUrl/$fileUploadFrontendSuffix/${envelope.id}/files/${UUID.randomUUID().toString}"
        val completeFileUploadUrl = s"$fileUploadUrl?$successRedirectUrl&$errorRedirectUrl"
        Future.successful(Some(completeFileUploadUrl))
      case _ =>
        fileUploadConnector.createEnvelope(userId).flatMap { response =>
          response.header("Location") match {
            case Some(locationHeader) =>
              locationHeader match {
                case envelopeIdPattern(id) =>
                  sessionService.cacheEnvelope(Envelope(id)).map {
                    case Some(_) =>
                      Logger.info(s"[UploadService][createFileUploadUrl] Envelope id obtained and cached for userId ($userId)")
                      val fileUploadUrl = s"$fileUploadFrontendBaseUrl/$fileUploadFrontendSuffix/$id/files/${UUID.randomUUID().toString}"
                      val completeFileUploadUrl = s"$fileUploadUrl?$successRedirectUrl&$errorRedirectUrl"
                      Some(completeFileUploadUrl)
                    case _ =>
                      Logger.error(s"[FileUploadController][get] failed to retrieve cache after storing the envelope for userId ($userId)")
                      None
                  }
                case _ =>
                  Logger.error(s"[UploadService][createFileUploadUrl] Failed to obtain an envelope id from location header for userId ($userId)")
                  Future.successful(None)
              }
            case _ =>
              Logger.error(s"[UploadService][createFileUploadUrl] Failed to find a location header in the response for userId ($userId)")
              Future.successful(None)
          }
        }.recover {
          case e: Throwable =>
            Logger.error(s"[UploadService][createFileUploadUrl] Failed to create envelope. ${e.getMessage}", e)
            None
        }
    }
  }

  def back: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) => Future.successful(previousPage("FileUploadController"))
        case Left(res) => res
      }
  }

  def uploadSuccess: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised.flatMap {
      case Right(userId) =>
        sessionService.fetchRasSession.flatMap {
          case Some(session) =>
            session.envelope match {
              case Some(envelope) =>
                shortLivedCache.createFileSession(userId,envelope.id).map {
                  case true =>
                    Logger.info(s"[FileUploadController][uploadSuccess] upload has been successful for userId ($userId)")
                    Ok(views.html.file_upload_successful())
                  case _ =>
                    Logger.error(s"[FileUploadController][uploadSuccess] failed to create file session for userId ($userId)")
                    Redirect(routes.ErrorController.renderGlobalErrorPage())
                }
              case _ =>
                Logger.error(s"[FileUploadController][uploadSuccess] no envelope exists in the session for userId ($userId)")
                Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
            }
          case _ =>
            Logger.error(s"[FileUploadController][uploadSuccess] session could not be retrieved for userId ($userId)")
            Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
        }
      case Left(resp) =>
        Logger.warn("[FileUploadController][uploadSuccess] user not authorised")
        resp
    }
  }

  def uploadError: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised.flatMap {
      case Right(_) =>

        val errorCode: String = request.getQueryString("errorCode").getOrElse("")
        val errorReason: String = request.getQueryString("reason").getOrElse("")
        val errorResponse: UploadResponse = UploadResponse(errorCode, Some(errorReason))

        sessionService.cacheUploadResponse(errorResponse).flatMap {
          case Some(_) =>
            Future.successful(Redirect(routes.FileUploadController.post()))
          case _ =>
            Future.successful(Redirect(routes.ErrorController.renderProblemUploadingFilePage()))
        }

      case Left(resp) =>
        Logger.warn("[FileUploadController][uploadError] user not authorised")
        resp
    }
  }

  def uploadInProgress: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised.flatMap {
      case Right(userId) =>
        shortLivedCache.fetchFileSession(userId).flatMap {
          case Some(fileSession) =>
            fileSession.resultsFile match {
              case Some(_) =>
                Logger.info("[FileUploadController][uploadInProgress] redirecting to file ready page")
                Future.successful(Redirect(routes.ChooseAnOptionController.renderFileReadyPage()))
              case _ =>
                Logger.info("[FileUploadController][uploadInProgress] calling cannot upload another file")
                Future.successful(Ok(views.html.cannot_upload_another_file()))
            }
          case _ =>
            Logger.info("[FileUploadController][uploadInProgress] redirecting to global error")
            Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
        }
      case Left(resp) =>
        Logger.warn("[FileUploadController][uploadInProgress] user not authorised")
        resp
    }
  }

  private def extractErrorReason(uploadResponse: Option[UploadResponse]):String ={
    uploadResponse match {
      case Some(response) =>
        response.code match {
          case "400" if response.reason.getOrElse("").contains(Messages("file.upload.empty.file.reason")) =>
            Logger.error("[FileUploadController][extractErrorReason] empty file")
            Messages("file.empty.error")
          case "400" =>
            Logger.error("[FileUploadController][extractErrorReason] bad request")
            Messages("upload.failed.error")
          case "404" =>
            Logger.error("[FileUploadController][extractErrorReason] envelope not found")
            Messages("upload.failed.error")
          case "413" =>
            Logger.error("[FileUploadController][extractErrorReason] file too large")
            Messages("file.large.error")
          case "415" =>
            Logger.error("[FileUploadController][extractErrorReason] file type other than the supported type")
            Messages("upload.failed.error")
          case "423" =>
            Logger.error("[FileUploadController][extractErrorReason] routing request has been made for this Envelope. Envelope is locked")
            Messages("upload.failed.error")
          case "" =>
            Logger.error("[FileUploadController][extractErrorReason] no error code returned")
            ""
          case _ =>
            Logger.error("[FileUploadController][extractErrorReason] unknown cause")
            Messages("upload.failed.error")
        }
      case _ => ""
    }
  }
}
