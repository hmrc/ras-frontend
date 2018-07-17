/*
 * Copyright 2018 HM Revenue & Customs
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
import config.{FrontendAuthConnector, RasContext, RasContextImpl}
import connectors.{ResidencyStatusAPIConnector, UserDetailsConnector}
import org.joda.time.DateTime
import play.api.http.HttpEntity
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment, Logger, Play}
import uk.gov.hmrc.auth.core.AuthConnector
import helpers.helpers.I18nHelper
import services.ShortLivedCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYearResolver
import models.FileUploadStatus._

import scala.concurrent.Future

object WhatDoYouWantToDoController extends WhatDoYouWantToDoController {
  // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  override val resultsFileConnector:ResidencyStatusAPIConnector = ResidencyStatusAPIConnector
  // $COVERAGE-ON$
}

trait WhatDoYouWantToDoController extends RasController with PageFlowController with I18nHelper {

  val resultsFileConnector:ResidencyStatusAPIConnector
  implicit val context: RasContext = RasContextImpl
  private val _contentType =   "application/csv"

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.determineFileStatus(userId).flatMap {
            fileStatus => Future.successful(Ok(views.html.what_do_you_want_to_do(fileStatus)))
          }
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][get] user not authorised")
          resp

      }
  }

//  def post = Action.async {
//    implicit request =>
//      isAuthorised.flatMap {
//        case Right(userId) =>
//          whatDoYouWantToDoForm.bindFromRequest.fold(
//            formWithErrors => {
//              Logger.error("[WhatDoYouWantToDoController][post] No option selected")
//              Future.successful(BadRequest(views.html.what_do_you_want_to_do(formWithErrors)))
//            },
//            whatDoYouWantToDo =>
//              sessionService.cacheWhatDoYouWantToDo(whatDoYouWantToDo.userChoice.get).flatMap {
//                case Some(session) =>
//                  session.userChoice match {
//                    case WhatDoYouWantToDo.SINGLE => Future.successful(Redirect(routes.MemberNameController.get()))
//                    case WhatDoYouWantToDo.BULK =>
//                      shortLivedCache.fetchFileSession(userId).flatMap {
//                        case Some(fileSession) =>
//                          fileSession.resultsFile match {
//                            case Some(_) =>
//                              Future.successful(Redirect(routes.WhatDoYouWantToDoController.renderFileReadyPage()))
//                            case _ =>
//                              Future.successful(Redirect(routes.FileUploadController.get))
//                          }
//                        case _ =>
//                          Future.successful(Redirect(routes.FileUploadController.get))
//                      }
//                    case WhatDoYouWantToDo.RESULT =>
//                      shortLivedCache.failedProcessingUploadedFile(userId).flatMap {
//                        case true =>
//                          Future.successful(Redirect(routes.ErrorController.renderProblemGettingResultsPage()))
//                        case _ =>
//                          Future.successful(Redirect(routes.WhatDoYouWantToDoController.renderUploadResultsPage()))
//                      }
//                    case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
//                  }
//                case _ =>
//                  Logger.error("[WhatDoYouWantToDoController][post] failed to retrieve session")
//                  Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
//              }
//          )
//        case Left(resp) =>
//          Logger.error("[WhatDoYouWantToDoController][post] user mot authorised")
//          resp
//      }
//  }

  def renderUploadResultsPage = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          sessionService.hasUserDimissedUrBanner().flatMap { urBannerDismissed =>
            shortLivedCache.fetchFileSession(userId).map {
              case Some(fileSession) =>
                fileSession.resultsFile match {
                  case Some(resultFile) =>
                    resultFile.uploadDate match {
                      case Some(timestamp) =>
                        val expiryDate = new DateTime(timestamp).plusDays(3)
                        val expiry = s"${expiryDate.toString("EEEE d MMMM yyyy")} at ${expiryDate.toString("H:mma").toLowerCase()}"
                        fileSession.userFile match {
                          case Some(callbackData) =>
                            val currentTaxYear = TaxYearResolver.currentTaxYear
                            val filename = ShortLivedCache.getDownloadFileName(fileSession)
                            Ok(views.html.upload_result(callbackData.fileId, expiry, isBeforeApr6(timestamp), currentTaxYear, filename, !urBannerDismissed))
                          case _ =>
                            Logger.error("[WhatDoYouWantToDoController][renderUploadResultsPage] failed to retrieve callback data")
                            Redirect(routes.ErrorController.renderGlobalErrorPage)
                        }
                      case _ =>
                        Logger.error("[WhatDoYouWantToDoController][renderUploadResultsPage] failed to retrieve upload timestamp")
                        Redirect(routes.ErrorController.renderGlobalErrorPage)
                    }
                  case _ =>
                    Logger.info("[WhatDoYouWantToDoController][renderUploadResultsPage] file upload in progress")
                    Redirect(routes.WhatDoYouWantToDoController.renderNoResultsAvailableYetPage)
                }
              case _ =>
                Logger.info("[WhatDoYouWantToDoController][renderUploadResultsPage] no results available")
                Redirect(routes.WhatDoYouWantToDoController.renderNoResultAvailablePage)
            }
          }
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][renderUploadResultsPage] user not authorised")
          resp
      }
  }

  def renderNoResultAvailablePage = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Logger.info("[WhatDoYouWantToDoController][renderNotResultAvailablePage] rendering no result available page")
          Future.successful(Ok(views.html.no_results_available()))
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][renderNotResultAvailablePage] user not authorised")
          resp
      }
  }

  def renderNoResultsAvailableYetPage = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Logger.info("[WhatDoYouWantToDoController][renderNotResultAvailableYetPage] rendering results not available page")
          Future.successful(Ok(views.html.results_not_available_yet()))
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][renderNotResultAvailableYetPage] user not authorised")
          resp
      }
  }

  def renderFileReadyPage = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).flatMap {
            case Some(fileSession) =>
              fileSession.resultsFile match {
                case Some(resultFile) =>
                  Future.successful(Ok(views.html.file_ready()))
                case _ =>
                  Logger.error("[WhatDoYouWantToDoController][renderFileReadyPage] session has no result file")
                  Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
              }
            case _ =>
              Logger.error("[WhatDoYouWantToDoController][renderFileReadyPage] failed to retrieve session")
              Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
          }
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][renderFileReadyPage] user not authorised")
          resp
      }
  }

  def getResultsFile(fileName:String) = Action.async {
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
                      Logger.error("[WhatDoYouWantToDoController][getResultsFile] filename empty")
                      Future.successful(Redirect(routes.ErrorController.fileNotAvailable))
                  }
                case _ =>
                  Logger.error("[WhatDoYouWantToDoController][getResultsFile] no result file found")
                  Future.successful(Redirect(routes.ErrorController.fileNotAvailable))
              }
            case _ =>
              Logger.error("[WhatDoYouWantToDoController][getResultsFile] no file session for User Id")
              Future.successful(Redirect(routes.ErrorController.fileNotAvailable))
          }
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][getResultsFile] user not authorised")
          resp
      }
  }

  private def getFile(fileName: String, userId: String, downloadFileName: String)(implicit hc: HeaderCarrier): Future[play.api.mvc.Result] = {
    resultsFileConnector.getFile(fileName, userId).map { response =>
      val dataContent: Source[ByteString, _] = StreamConverters.fromInputStream(() => response.get)

      Ok.sendEntity(HttpEntity.Streamed(dataContent, None, Some(_contentType)))
        .withHeaders(CONTENT_DISPOSITION -> s"""attachment; filename="${downloadFileName}-results.csv"""",
          // CONTENT_LENGTH -> s"${fileData.get.length}",
          CONTENT_TYPE -> _contentType)

    }.recover {
      case ex: Throwable => Logger.error("Request failed with Exception " + ex.getMessage + " for file -> " + fileName)
        Redirect(routes.ErrorController.renderGlobalErrorPage)
    }
  }

  private def isBeforeApr6(timestamp: Long) : Boolean = {
    val uploadDate = new DateTime(timestamp)
    uploadDate.isBefore(DateTime.parse(s"${uploadDate.getYear()}-04-06"))
  }
}
