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

package controllers

import config.ApplicationConfig
import connectors.UpscanInitiateConnector
import models.upscan.UploadId
import models.{Envelope, UploadResponse}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request}
import services.{FilesSessionService, SessionCacheService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.UnanchoredRegex

class FileUploadController @Inject()(upscanInitiateConnector: UpscanInitiateConnector,
                                     val authConnector: DefaultAuthConnector,
                                     val filesSessionService: FilesSessionService,
                                     val sessionService: SessionCacheService,
                                     val mcc: MessagesControllerComponents,
                                     implicit val appConfig: ApplicationConfig,
                                     fileUploadView: views.html.file_upload,
                                     fileUploadSuccessView: views.html.file_upload_successful,
                                     cannotUploadAnotherView: views.html.cannot_upload_another_file
																		) extends FrontendController(mcc) with RasController with PageFlowController with Logging {

	implicit val ec: ExecutionContext = mcc.executionContext

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(userId) =>
          sessionService.fetchRasSession().flatMap {
            case Some(session) =>
              filesSessionService.isFileInProgress(userId).flatMap {
                case true =>
                  logger.info(s"[FileUploadController][get] a file is still processing for userId ($userId) " +
                    s"so another could not be uploaded")
                  Future.successful(Redirect(routes.FileUploadController.uploadInProgress))
                case _ =>
                  createFileUploadUrl(userId).flatMap {
                    case Some(url) =>
                      logger.info(s"[FileUploadController][get] form url created successfully for userId ($userId)")
                      val error = extractErrorReason(session.uploadResponse)
                      if(error == "upload.failed.error"){
                        sessionService.cacheUploadResponse(UploadResponse("",None)).map {
                          case Some(_) =>
                            Redirect(routes.ErrorController.renderProblemUploadingFilePage)
                          case _ =>
                            logger.error(s"[FileUploadController][get] failed to obtain a session for userId ($userId)")
                            Redirect(routes.ErrorController.renderGlobalErrorPage)
                        }
                      }
                      else {
                        sessionService.resetCacheUploadResponse()
                        Future.successful(Ok(fileUploadView(url,error)))
                      }
                    case _ =>
                      logger.error(s"[FileUploadController][get] failed to obtain a form url using existing envelope " +
                        s"for userId ($userId)")
                      Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
                  }
              }
            case _ =>
              createFileUploadUrl(userId).flatMap {
                case Some(url) =>
                  logger.info(s"[FileUploadController][get] stored new envelope id successfully for userId ($userId)")
                  Future.successful(Ok(fileUploadView(url,"")))
                case _ =>
                  logger.error(s"[FileUploadController][get] failed to obtain a form url using new envelope for userId ($userId)")
                  Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
              }
          }.recover {
            case e: Throwable =>
              logger.error(s"[FileUploadController][get] failed to fetch ras session for userId ($userId) - $e")
              Redirect(routes.ErrorController.renderGlobalErrorPage)
          }
        case Left(resp) =>
          logger.warn("[FileUploadController][get] user not authorised")
          resp
      }
  }


  def createFileUploadUrl(userId: String)(implicit hc:HeaderCarrier, request: Request[_]) = {
    val uploadId = UploadId.generate

    def urlToString(c: Call): String = appConfig.uploadRedirectTargetBase + c.url


    val successRedirectUrl  = controllers.routes.FileUploadController.uploadSuccess
    val errorRedirectUrl = controllers.routes.FileUploadController.uploadError

    (for {
      upscanInitiateResponse <- upscanInitiateConnector.initiateUpscan(userId, Some(urlToString(successRedirectUrl)), Some(urlToString(errorRedirectUrl)))
      session <- sessionService.cacheEnvelope(Envelope(upscanInitiateResponse.fileReference.reference))
    } yield (upscanInitiateResponse, session)).map {
      case (uir, Some(_)) => Some(uir)
      case _ => None
    }
  }

//  def createFileUploadUrl(envelope: Option[Envelope], userId: String)(implicit hc:HeaderCarrier): Future[Option[String]] = {
//    lazy val rasFrontendBaseUrl: String = appConfig.rasFrontendBaseUrl
//    lazy val rasFrontendUrlSuffix: String = appConfig.rasFrontendUrlSuffix
//    lazy val fileUploadFrontendBaseUrl: String = appConfig.fileUploadFrontendBaseUrl
//    lazy val fileUploadFrontendSuffix: String = appConfig.fileUploadFrontendSuffix
//    val envelopeIdPattern: UnanchoredRegex = "envelopes/([\\w\\d-]+)$".r.unanchored
//    val successRedirectUrl: String = s"redirect-success-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/file-uploaded"
//    val errorRedirectUrl: String = s"redirect-error-url=$rasFrontendBaseUrl/$rasFrontendUrlSuffix/file-upload-failed"
//
//    envelope match {
//      case Some(envelope) =>
//        val fileUploadUrl = s"$fileUploadFrontendBaseUrl/$fileUploadFrontendSuffix/${envelope.id}/files/${UUID.randomUUID().toString}"
//        val completeFileUploadUrl = s"$fileUploadUrl?$successRedirectUrl&$errorRedirectUrl"
//        Future.successful(Some(completeFileUploadUrl))
//      case _ =>
//        fileUploadConnector.createEnvelope(userId).flatMap { response =>
//          response.header("Location") match {
//            case Some(locationHeader) =>
//              locationHeader match {
//                case envelopeIdPattern(id) =>
//                  sessionService.cacheEnvelope(Envelope(id)).map {
//                    case Some(_) =>
//                      logger.info(s"[UploadService][createFileUploadUrl] Envelope id obtained and cached for userId ($userId)")
//                      val fileUploadUrl = s"$fileUploadFrontendBaseUrl/$fileUploadFrontendSuffix/$id/files/${UUID.randomUUID().toString}"
//                      val completeFileUploadUrl = s"$fileUploadUrl?$successRedirectUrl&$errorRedirectUrl"
//                      Some(completeFileUploadUrl)
//                    case _ =>
//                      logger.error(s"[FileUploadController][get] failed to retrieve cache after storing the envelope for userId ($userId)")
//                      None
//                  }
//                case _ =>
//                  logger.error(s"[UploadService][createFileUploadUrl] Failed to obtain an envelope id from location header for userId ($userId)")
//                  Future.successful(None)
//              }
//            case _ =>
//              logger.error(s"[UploadService][createFileUploadUrl] Failed to find a location header in the response for userId ($userId)")
//              Future.successful(None)
//          }
//        }.recover {
//          case e: Throwable =>
//            logger.error(s"[UploadService][createFileUploadUrl] Failed to create envelope. ${e.getMessage}", e)
//            None
//        }
//    }
//  }

  def back: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(_) => Future.successful(previousPage("FileUploadController"))
        case Left(res) => res
      }
  }

  def uploadSuccess: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised().flatMap {
      case Right(userId) =>
        sessionService.fetchRasSession().flatMap {
          case Some(session) =>
            session.envelope match {
              case Some(envelope) =>
                filesSessionService.createFileSession(userId,envelope.id).map {
                  case true =>
                    logger.info(s"[FileUploadController][uploadSuccess] upload has been successful for userId ($userId)")
                    Ok(fileUploadSuccessView())
                  case _ =>
                    logger.error(s"[FileUploadController][uploadSuccess] failed to create file session for userId ($userId)")
                    Redirect(routes.ErrorController.renderGlobalErrorPage)
                }
              case _ =>
                logger.error(s"[FileUploadController][uploadSuccess] no envelope exists in the session for userId ($userId)")
                Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
            }
          case _ =>
            logger.error(s"[FileUploadController][uploadSuccess] session could not be retrieved for userId ($userId)")
            Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
        }
      case Left(resp) =>
        logger.warn("[FileUploadController][uploadSuccess] user not authorised")
        resp
    }
  }

  def uploadError: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised().flatMap {
      case Right(_) =>
        val errorCode: String = request.getQueryString("errorCode").getOrElse("")
        val errorReason: String = request.getQueryString("errorMessage").getOrElse("")
        val errorResponse: UploadResponse = UploadResponse(errorCode, Some(errorReason))

        sessionService.cacheUploadResponse(errorResponse).flatMap {
          case Some(_) =>
            Future.successful(Redirect(routes.FileUploadController.get))
          case _ =>
            Future.successful(Redirect(routes.ErrorController.renderProblemUploadingFilePage))
        }

      case Left(resp) =>
        logger.warn("[FileUploadController][uploadError] user not authorised")
        resp
    }
  }

  def uploadInProgress: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised().flatMap {
      case Right(userId) =>
        filesSessionService.fetchFileSession(userId).flatMap {
          case Some(fileSession) =>
            fileSession.resultsFile match {
              case Some(_) =>
                logger.info("[FileUploadController][uploadInProgress] redirecting to file ready page")
                Future.successful(Redirect(routes.ChooseAnOptionController.renderFileReadyPage))
              case _ =>
                logger.info("[FileUploadController][uploadInProgress] calling cannot upload another file")
                Future.successful(Ok(cannotUploadAnotherView()))
            }
          case _ =>
            logger.info("[FileUploadController][uploadInProgress] redirecting to global error")
            Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
        }
      case Left(resp) =>
        logger.warn("[FileUploadController][uploadInProgress] user not authorised")
        resp
    }
  }

  private def extractErrorReason(uploadResponse: Option[UploadResponse]):String ={
		val fileUploadEmptyFileReason= "Envelope does not allow zero length files, and submitted file has length 0"

		uploadResponse match {
      case Some(response) =>
        response.code match {
          case "400" if response.reason.getOrElse("").contains(fileUploadEmptyFileReason) =>
            logger.error("[FileUploadController][extractErrorReason] empty file")
            "file.empty.error"
          case "400" =>
            logger.error("[FileUploadController][extractErrorReason] bad request")
            "upload.failed.error"
          case "404" =>
            logger.error("[FileUploadController][extractErrorReason] envelope not found")
            "upload.failed.error"
          case "413" =>
            logger.error("[FileUploadController][extractErrorReason] file too large")
            "file.large.error"
          case "415" =>
            logger.error("[FileUploadController][extractErrorReason] file type other than the supported type")
            "upload.failed.error"
          case "423" =>
            logger.error("[FileUploadController][extractErrorReason] routing request has been made for this Envelope. Envelope is locked")
            "upload.failed.error"
          case "" =>
            logger.error("[FileUploadController][extractErrorReason] no error code returned")
            ""
          case other @_ =>
            logger.error(s"[FileUploadController][extractErrorReason] unknown cause: $other")
            "upload.failed.error"
        }
      case _ => ""
    }
  }
}
