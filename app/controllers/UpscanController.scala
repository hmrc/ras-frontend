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
import models.upscan.UpscanInitiateResponse
import models.{File, RasSession, UploadResponse}
import play.api.Logging
import play.api.i18n.Messages.implicitMessagesProviderToMessages
import play.api.mvc._
import services.{FilesSessionService, SessionCacheService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpscanController @Inject()(upscanInitiateConnector: UpscanInitiateConnector,
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

  def get: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised().flatMap { result =>
      if (result.isRight) {
        val userId = result.right.get
        val fileUploadUrl: Future[Option[UpscanInitiateResponse]] = createFileUploadUrl(userId)
        val sessionFetchResult = sessionService.fetchRasSession()
        sessionFetchResult.flatMap { sessionOption =>
          if (sessionOption.isDefined) {
            val session = sessionOption.get
            if (session.isInstanceOf[RasSession]) {
              val rasSession = session.asInstanceOf[RasSession]
              processRasSession(rasSession, userId, fileUploadUrl)
            } else {
              redirectWithNoRasSession(userId)
            }
          } else {
            redirectWithNoRasSession(userId)
          }
        }.recover {
          case e: Throwable =>
            logger.error(s"[UpscanController][get] failed to fetch ras session for userId ($userId) - $e")
            Redirect(routes.ErrorController.renderGlobalErrorPage)
        }
      } else {
        val resp = result.left.get
        logger.warn("[UpscanController][get] user not authorised")
        resp
      }
    }
  }

  def redirectWithNoRasSession(userId: String)(implicit request: MessagesRequest[AnyContent]): Future[Result] = {
    val fileUploadUrlFuture = createFileUploadUrl(userId)
    fileUploadUrlFuture.flatMap { urlOption =>
      if (urlOption.isDefined) {
        val url = urlOption.get
        logger.info(s"[UpscanController][get] stored new reference successfully for userId ($userId)")
        Future.successful(Ok(fileUploadView(url, "")))
      } else {
        logger.error(s"[UpscanController][get] failed to obtain a form url using new reference for userId ($userId)")
        Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
      }
    }
  }

  def processRasSession(
                         session: RasSession,
                         userId: String,
                         fileUploadUrl: Future[Option[UpscanInitiateResponse]])(implicit hc: HeaderCarrier, request: MessagesRequest[AnyContent]): Future[Result] = {

    filesSessionService.isFileInProgress(userId).flatMap {
      case true =>
        logger.info(s"[UpscanController][get] a file is still processing for userId ($userId) " +
          s"so another could not be uploaded")
        Future.successful(Redirect(routes.UpscanController.uploadInProgress))
      case _ => fileUploadUrl.flatMap(url => processFileUploadUrl(url, session, userId))
    }
  }

  def processFileUploadUrl(
                            url: Option[UpscanInitiateResponse],
                            session: RasSession,
                            userId: String)(implicit request: MessagesRequest[AnyContent]): Future[Result] = {
    if (url.isDefined) {
      val value = url.get
      val error = extractErrorReason(session.uploadResponse)
      if (error == "upload.failed.error") {
        sessionService.cacheUploadResponse(UploadResponse("", None)).map {
          case Some(_) =>
            Redirect(routes.ErrorController.renderProblemUploadingFilePage)
          case _ =>
            logger.error(s"[UpscanController][get] failed to obtain a session for userId ($userId)")
            Redirect(routes.ErrorController.renderGlobalErrorPage)
        }
      } else {
        sessionService.resetCacheUploadResponse()
        Future.successful(Ok(fileUploadView(value, error)))
      }
    } else {
      logger.error(s"[UpscanController][get] failed to obtain a form url using existing file reference " +
        s"for userId ($userId)")
      Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
    }
  }

  def createFileUploadUrl(userId: String)(implicit hc:HeaderCarrier, request: Request[_]) = {
    def urlToString(c: Call): String = appConfig.uploadRedirectTargetBase + c.url

    val successRedirectUrl  = controllers.routes.UpscanController.uploadSuccess
    val errorRedirectUrl = controllers.routes.UpscanController.uploadError

    (for {
      upscanInitiateResponse <- upscanInitiateConnector.initiateUpscan(userId, Some(urlToString(successRedirectUrl)), Some(urlToString(errorRedirectUrl)))
      session <- sessionService.cacheFile(File(upscanInitiateResponse.fileReference.reference))
    } yield (upscanInitiateResponse, session)).map {
      case (uir, Some(_)) => Some(uir)
      case _ => None
    }
  }

  def back: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(_) => Future.successful(previousPage("UpscanController"))
        case Left(res) => res
      }
  }

  def uploadSuccess: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised().flatMap { result =>
      if (result.isRight) {
        val userId = result.right.get
        sessionService.fetchRasSession().flatMap { sessionOption =>
          if (sessionOption.isDefined) {
            val session = sessionOption.get
            if (session.file.isDefined) {
              val file = session.file.get
              filesSessionService.createFileSession(userId, file.id).map { creationResult =>
                if (creationResult) {
                  logger.info(s"[UpscanController][uploadSuccess] upload has been successful for userId ($userId)")
                  Ok(fileUploadSuccessView())
                } else {
                  logger.error(s"[UpscanController][uploadSuccess] failed to create file session for userId ($userId)")
                  Redirect(routes.ErrorController.renderGlobalErrorPage)
                }
              }
            } else {
              logger.error(s"[UpscanController][uploadSuccess] no file reference exists in the session for userId ($userId)")
              Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
            }
          } else {
            logger.error(s"[UpscanController][uploadSuccess] session could not be retrieved for userId ($userId)")
            Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
          }
        }
      } else {
        val resp = result.left.get
        logger.warn("[UpscanController][uploadSuccess] user not authorised")
        resp
      }
    }
  }

  def uploadError: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised().flatMap { result =>
      if (result.isRight) {
        val errorCode: String = request.getQueryString("errorCode").getOrElse("")
        val errorReason: String = request.getQueryString("errorMessage").getOrElse("")
        val errorResponse: UploadResponse = UploadResponse(errorCode, Some(errorReason))

        sessionService.cacheUploadResponse(errorResponse).flatMap {
          case Some(_) =>
            Future.successful(Redirect(routes.UpscanController.get))
          case _ =>
            Future.successful(Redirect(routes.ErrorController.renderProblemUploadingFilePage))
        }
      } else {
        val resp = result.left.get
        logger.warn("[UpscanController][uploadError] user not authorised")
        resp
      }
    }
  }

  def uploadInProgress: Action[AnyContent] = Action.async { implicit request =>
    isAuthorised().flatMap { result =>
      if (result.isRight) {
        val userId = result.right.get
        filesSessionService.fetchFileSession(userId).flatMap {
          case Some(fileSession) =>
            if (fileSession.resultsFile.isDefined) {
              logger.info("[UpscanController][uploadInProgress] redirecting to file ready page")
              Future.successful(Redirect(routes.ChooseAnOptionController.renderFileReadyPage))
            } else {
              logger.info("[UpscanController][uploadInProgress] calling cannot upload another file")
              Future.successful(Ok(cannotUploadAnotherView()))
            }
          case None =>
            logger.info("[UpscanController][uploadInProgress] redirecting to global error")
            Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
        }
      } else {
        val resp = result.left.get
        logger.warn("[UpscanController][uploadInProgress] user not authorised")
        resp
      }
    }
  }

  private def extractErrorReason(uploadResponse: Option[UploadResponse]): String = {
    if (uploadResponse.isDefined) {
      val response = uploadResponse.get
      val errorCode = response.code
      if (errorCode == "EntityTooLarge") {
        logger.error("[UpscanController][extractErrorReason] file too large")
        "file.large.error"
      } else if (errorCode == "EntityTooSmall") {
        logger.error("[UpscanController][extractErrorReason] file too small")
        "file.empty.error"
      } else if (errorCode.isEmpty) {
        logger.info("[UpscanController][extractErrorReason] no error code returned")
        ""
      } else {
        logger.error(s"[UpscanController][extractErrorReason] returned error code: $errorCode")
        "upload.failed.error"
      }
    } else {
      ""
    }
  }
}
