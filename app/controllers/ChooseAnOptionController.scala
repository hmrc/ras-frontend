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

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import config.ApplicationConfig
import connectors.ResidencyStatusAPIConnector
import javax.inject.Inject
import models.FileUploadStatus._
import models.{FileSession, FileUploadStatus}
import org.joda.time.DateTime
import play.api.Logger
import play.api.http.HttpEntity
import play.api.mvc._
import services.{SessionService, ShortLivedCache, TaxYearResolver}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ChooseAnOptionController @Inject()(resultsFileConnector: ResidencyStatusAPIConnector,
                                         val authConnector: DefaultAuthConnector,
                                         val shortLivedCache: ShortLivedCache,
                                         val sessionService: SessionService,
                                         val mcc: MessagesControllerComponents,
                                         implicit val appConfig: ApplicationConfig,
                                         chooseAnOptionView: views.html.choose_an_option,
                                         fileReadyView: views.html.file_ready,
                                         uploadResultView: views.html.upload_result,
                                         resultsNotAvailableYetView: views.html.results_not_available_yet,
                                         noResultsAvailableView: views.html.no_results_available
																				) extends FrontendController(mcc) with PageFlowController {

	implicit val ec: ExecutionContext = mcc.executionContext
  private val _contentType =   "application/csv"

  def get: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).flatMap { fileSession =>
            shortLivedCache.determineFileStatus(userId).flatMap {
              fileStatus =>
                Logger.info(s"[ChooseAnOptionController][get] determine file status returned $fileStatus")
                Future.successful(Ok(chooseAnOptionView(fileStatus, getHelpDate(fileStatus, fileSession))))
            }
          }
        case Left(resp) =>
          Logger.warn("[ChooseAnOptionController][get] user not authorised")
          resp
      }
  }

  private[controllers] def getHelpDate(fileStatus: FileUploadStatus.Value, fileSession: Option[FileSession]): Option[String] = {
    fileSession match {
      case Some(fileSession) =>
        fileStatus match {
          case Ready => Some(formattedExpiryDate(fileSession.resultsFile.get.uploadDate.get)) // will always have a resultsfile and date if ready
          case InProgress => Some(formattedUploadDate(fileSession.uploadTimeStamp.get)) // will always have an upload date time if in progress
          case _ => None
        }
      case _ => None
    }
  }

  def formattedExpiryDate(timestamp: Long): String = {
    val expiryDate = new DateTime(timestamp).plusDays(3)
    s"${expiryDate.toString("H:mma").toLowerCase()} on ${expiryDate.toString("EEEE d MMMM yyyy")}"
  }

  private def formattedUploadDate(timestamp: Long): String = {
    val uploadDate = new DateTime(timestamp)

    val todayOrYesterday = if (uploadDate.toLocalDate.isEqual(DateTime.now.toLocalDate)) {
      "today"
    } else {
      "yesterday"
    }
		s"$todayOrYesterday at ${uploadDate.toString("h:mma").toLowerCase()}"
  }

  def renderUploadResultsPage: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).map {
            case Some(fileSession) =>
              fileSession.resultsFile match {
                case Some(resultFile) =>
                  resultFile.uploadDate match {
                    case Some(timestamp) =>
                      fileSession.userFile match {
                        case Some(callbackData) =>
                          val currentTaxYear = TaxYearResolver.currentTaxYear
                          val filename = shortLivedCache.getDownloadFileName(fileSession)
                          Ok(uploadResultView(callbackData.fileId, formattedExpiryDate(timestamp), isBeforeApr6(timestamp), currentTaxYear, filename))
                        case _ =>
                          Logger.error("[ChooseAnOptionController][renderUploadResultsPage] failed to retrieve callback data")
                          Redirect(routes.ErrorController.renderGlobalErrorPage())
                      }
                    case _ =>
                      Logger.error("[ChooseAnOptionController][renderUploadResultsPage] failed to retrieve upload timestamp")
                      Redirect(routes.ErrorController.renderGlobalErrorPage())
                  }
                case _ =>
                  Logger.info("[ChooseAnOptionController][renderUploadResultsPage] file upload in progress")
                  Redirect(routes.ChooseAnOptionController.renderNoResultsAvailableYetPage())
              }
            case _ =>
              Logger.info("[ChooseAnOptionController][renderUploadResultsPage] no results available")
              Redirect(routes.ChooseAnOptionController.renderNoResultAvailablePage())
          }
        case Left(resp) =>
          Logger.warn("[ChooseAnOptionController][renderUploadResultsPage] user not authorised")
          resp
      }
  }

  def renderNoResultAvailablePage: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).flatMap {
            case Some(fileSession) =>
              fileSession.resultsFile match {
                case Some(_) =>
                  Logger.info("[ChooseAnOptionController][renderNotResultAvailablePage] there is a file ready, rendering results page")
                  Future.successful(Redirect(routes.ChooseAnOptionController.renderUploadResultsPage()))
                case _ =>
                  Logger.info("[ChooseAnOptionController][renderNotResultAvailablePage] there is a file in progress, rendering results-not-available page")
                  Future.successful(Redirect(routes.ChooseAnOptionController.renderNoResultsAvailableYetPage()))
              }
            case _ =>
              Logger.info("[ChooseAnOptionController][renderNotResultAvailablePage] rendering no result available page")
              Future.successful(Ok(noResultsAvailableView()))
          }
        case Left(resp) =>
          Logger.warn("[ChooseAnOptionController][renderNotResultAvailablePage] user not authorised")
          resp
      }
  }

  def renderNoResultsAvailableYetPage: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).flatMap {
            case Some(fileSession) =>
              fileSession.resultsFile match {
                case Some(_) =>
                  Logger.info("[ChooseAnOptionController][renderNotResultAvailableYetPage] redirecting to results page")
                  Future.successful(Redirect(routes.ChooseAnOptionController.renderUploadResultsPage()))
                case _ =>
                  Logger.info("[ChooseAnOptionController][renderNotResultAvailableYetPage] rendering results not available page")
                  Future.successful(Ok(resultsNotAvailableYetView()))
              }
            case _ =>
              Logger.info("[ChooseAnOptionController][renderNotResultAvailableYetPage] redirecting to results not available")
              Future.successful(Redirect(routes.ChooseAnOptionController.renderNoResultAvailablePage()))
          }
        case Left(resp) =>
          Logger.warn("[ChooseAnOptionController][renderNotResultAvailableYetPage] user not authorised")
          resp
      }
  }

  def renderFileReadyPage: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).flatMap {
            case Some(fileSession) =>
              fileSession.resultsFile match {
                case Some(_) =>
                  Future.successful(Ok(fileReadyView()))
                case _ =>
                  Logger.error("[ChooseAnOptionController][renderFileReadyPage] session has no result file")
                  Future.successful(Redirect(routes.FileUploadController.uploadInProgress()))
              }
            case _ =>
              Logger.error("[ChooseAnOptionController][renderFileReadyPage] failed to retrieve session")
              Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
          }
        case Left(resp) =>
          Logger.warn("[ChooseAnOptionController][renderFileReadyPage] user not authorised")
          resp
      }
  }

  def getResultsFile(fileName:String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).flatMap {
            case Some(fileSession) =>
              fileSession.resultsFile match {
                case Some(resultFile) =>
                  resultFile.filename match {
                    case Some(name) if name == fileName =>
                      shortLivedCache.removeFileSessionFromCache(userId)
                      getFile(fileName, userId, shortLivedCache.getDownloadFileName(fileSession))
                    case _ =>
                      Logger.error("[ChooseAnOptionController][getResultsFile] filename empty")
                      Future.successful(Redirect(routes.ErrorController.fileNotAvailable()))
                  }
                case _ =>
                  Logger.error("[ChooseAnOptionController][getResultsFile] no result file found")
                  Future.successful(Redirect(routes.ErrorController.fileNotAvailable()))
              }
            case _ =>
              Logger.error("[ChooseAnOptionController][getResultsFile] no file session for User Id")
              Future.successful(Redirect(routes.ErrorController.fileNotAvailable()))
          }
        case Left(resp) =>
          Logger.warn("[ChooseAnOptionController][getResultsFile] user not authorised")
          resp
      }
  }

  private def getFile(fileName: String, userId: String, downloadFileName: String)(implicit hc: HeaderCarrier): Future[play.api.mvc.Result] = {
    resultsFileConnector.getFile(fileName, userId).map { response =>
      val dataContent: Source[ByteString, _] = StreamConverters.fromInputStream(() => response.get)
        .watchTermination()((_, futDone) => futDone.onComplete {
          case Success(_) =>
            Logger.info(s"[ChooseAnOptionController][getFile] File with name $fileName has started to delete")
            resultsFileConnector.deleteFile(fileName, userId)
          case Failure(f) =>
            Logger.warn(s"[ChooseAnOptionController][getFile] File with name $fileName was not deleted - $f")
        })

      Ok.sendEntity(HttpEntity.Streamed(dataContent, None, Some(_contentType)))
        .withHeaders(CONTENT_DISPOSITION -> s"""attachment; filename="$downloadFileName-results.csv"""",
          // CONTENT_LENGTH -> s"${fileData.get.length}",
          CONTENT_TYPE -> _contentType)

    }.recover {
      case ex: Throwable => Logger.error("[ChooseAnOptionController][getFile] Request failed with Exception " + ex.getMessage + " for file -> " + fileName)
        Redirect(routes.ErrorController.renderGlobalErrorPage())
    }
  }

  private def isBeforeApr6(timestamp: Long) : Boolean = {
    val uploadDate = new DateTime(timestamp)
    uploadDate.isBefore(DateTime.parse(s"${uploadDate.getYear}-04-06"))
  }
}
