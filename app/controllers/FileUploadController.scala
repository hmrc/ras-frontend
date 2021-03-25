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

package controllers

import java.util.UUID

import config.ApplicationConfig
import connectors.FileUploadConnector
import javax.inject.Inject
import models.{Envelope, UploadResponse}
import play.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.UnanchoredRegex

class FileUploadController @Inject()(fileUploadConnector: FileUploadConnector,
																		 val authConnector: DefaultAuthConnector,
																		 val shortLivedCache: ShortLivedCache,
																		 val sessionService: SessionService,
																		 val mcc: MessagesControllerComponents,
																		 implicit val appConfig: ApplicationConfig,
                                     fileUploadView: views.html.file_upload,
                                     fileUploadSuccessView: views.html.file_upload_successful,
                                     cannotUploadAnotherView: views.html.cannot_upload_another_file
																		) extends FrontendController(mcc) with RasController with PageFlowController {

	implicit val ec: ExecutionContext = mcc.executionContext

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          sessionService.fetchRasSession().flatMap {
            case Some(session) =>
              shortLivedCache.isFileInProgress(userId).flatMap {
                case true =>
                  Logger.info(s"[FileUploadController][get] a file is still processing for userId ($userId) " +
                    s"so another could not be uploaded")
                  Future.successful(Redirect(routes.FileUploadController.uploadInProgress()))
                case _ =>
                  createFileUploadUrl(session.envelope, userId)(request, hc).flatMap {
                    case Some(url) =>
                      Logger.info(s"[FileUploadController][get] form url created successfully for userId ($userId)")
                      val error = extractErrorReason(session.uploadResponse)
                      if(error == "upload.failed.error"){
                        sessionService.cacheUploadResponse(UploadResponse("",None)).map {
                          case Some(_) =>
                            Redirect(routes.ErrorController.renderProblemUploadingFilePage())
                          case _ =>
                            Logger.error(s"[FileUploadController][get] failed to obtain a session for userId ($userId)")
                            Redirect(routes.ErrorController.renderGlobalErrorPage())
                        }
                      }
                      else {
                        sessionService.resetCacheUploadResponse()
                        Future.successful(Ok(fileUploadView(url,error)))
                      }
                    case _ =>
                      Logger.error(s"[FileUploadController][get] failed to obtain a form url using existing envelope " +
                        s"for userId ($userId)")
                      Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
                  }
              }
            case _ =>
              createFileUploadUrl(None, userId)(request, hc).flatMap {
                case Some(url) =>
                  Logger.info(s"[FileUploadController][get] stored new envelope id successfully for userId ($userId)")
                  Future.successful(Ok(fileUploadView(url,"")))
                case _ =>
                  Logger.error(s"[FileUploadController][get] failed to obtain a form url using new envelope for userId ($userId)")
                  Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
              }
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
                    Ok(fileUploadSuccessView())
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
            Future.successful(Redirect(routes.FileUploadController.get()))
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
                Future.successful(Ok(cannotUploadAnotherView()))
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
		val fileUploadEmptyFileReason= "Envelope does not allow zero length files, and submitted file has length 0"

		uploadResponse match {
      case Some(response) =>
        response.code match {
          case "400" if response.reason.getOrElse("").contains(fileUploadEmptyFileReason) =>
            Logger.error("[FileUploadController][extractErrorReason] empty file")
            "file.empty.error"
          case "400" =>
            Logger.error("[FileUploadController][extractErrorReason] bad request")
            "upload.failed.error"
          case "404" =>
            Logger.error("[FileUploadController][extractErrorReason] envelope not found")
            "upload.failed.error"
          case "413" =>
            Logger.error("[FileUploadController][extractErrorReason] file too large")
            "file.large.error"
          case "415" =>
            Logger.error("[FileUploadController][extractErrorReason] file type other than the supported type")
            "upload.failed.error"
          case "423" =>
            Logger.error("[FileUploadController][extractErrorReason] routing request has been made for this Envelope. Envelope is locked")
            "upload.failed.error"
          case "" =>
            Logger.error("[FileUploadController][extractErrorReason] no error code returned")
            ""
          case _ =>
            Logger.error("[FileUploadController][extractErrorReason] unknown cause")
            "upload.failed.error"
        }
      case _ => ""
    }
  }
}
