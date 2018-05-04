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
import forms.WhatDoYouWantToDoForm.whatDoYouWantToDoForm
import helpers.helpers.I18nHelper
import models.WhatDoYouWantToDo
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYearResolver

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

  val fileIsInProgress = true
  val noFileInProgress = false
  val readyForDownload = true
  val notReadyForDownload = false

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.isFileInProgress(userId).flatMap {
            case true =>
              shortLivedCache.fetchFileSession(userId).map {
                case Some(fileSession) =>
                  fileSession.uploadTimeStamp match {
                    case Some(timestamp) =>
                      fileSession.userFile match {
                        case Some(callbackData) =>
                          Ok(views.html.what_do_you_want_to_do(whatDoYouWantToDoForm,fileIsInProgress, callbackData.fileId, readyForDownload))
                        case _ =>
                          Ok(views.html.what_do_you_want_to_do(whatDoYouWantToDoForm,fileIsInProgress, "", notReadyForDownload))
                      }
                    case _ =>
                      Logger.error("[WhatDoYouWantToDoController][get] no timestamp retrieved")
                      Redirect(routes.ErrorController.renderGlobalErrorPage)
                  }
                case _ =>
                  Logger.error("[WhatDoYouWantToDoController][get] failed to retrieve file session")
                  Redirect(routes.ErrorController.renderGlobalErrorPage)
              }
            case _ =>
              Future.successful(Ok(views.html.what_do_you_want_to_do(whatDoYouWantToDoForm,noFileInProgress,"",noFileInProgress)))
          }
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][get] user not authorised")
          resp
      }
  }

  def post = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          whatDoYouWantToDoForm.bindFromRequest.fold(
            formWithErrors => {
              Logger.error("[WhatDoYouWantToDoController][post] No option selected")
              Future.successful(BadRequest(views.html.what_do_you_want_to_do(formWithErrors,noFileInProgress,"",notReadyForDownload)))
            },
            whatDoYouWantToDo =>
              sessionService.cacheWhatDoYouWantToDo(whatDoYouWantToDo.userChoice.get).flatMap {
                case Some(session) =>
                  session.userChoice match {
                    case WhatDoYouWantToDo.SINGLE => Future.successful(Redirect(routes.MemberNameController.get()))
                    case WhatDoYouWantToDo.BULK => Future.successful(Redirect(routes.FileUploadController.get))
                    case WhatDoYouWantToDo.RESULT =>
                      shortLivedCache.failedProcessingUploadedFile(userId).flatMap {
                        case true =>
                          Future.successful(Redirect(routes.ErrorController.renderProblemGettingResultsPage()))
                        case _ =>
                          Future.successful(Redirect(routes.WhatDoYouWantToDoController.renderUploadResultsPage()))
                      }
                    case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
                  }
                case _ =>
                  Logger.error("[WhatDoYouWantToDoController][post] failed to retrieve session")
                  Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
              }
          )
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][post] user mot authorised")
          resp
      }
  }

  def renderUploadResultsPage = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          shortLivedCache.fetchFileSession(userId).map {
            case Some(fileSession) =>
              fileSession.resultsFile match {
                case Some(resultFile) =>
                  resultFile.uploadDate match {
                    case Some(timestamp) =>
                      val expiryDate = new DateTime(timestamp).plusDays(3)
                      val expiry = s"${expiryDate.toString("EEEE d MMMM yyyy")} at ${expiryDate.toString("HH:mma").toLowerCase()}"
                      fileSession.userFile match {
                        case Some(callbackData) =>
                          val currentTaxYear = TaxYearResolver.currentTaxYear
                          Ok(views.html.upload_result(callbackData.fileId, expiry, isBeforeApr6(timestamp), currentTaxYear))
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
                  Redirect(routes.ErrorController.renderNoResultsAvailableYetPage)
              }
            case _ =>
              Logger.info("[WhatDoYouWantToDoController][renderUploadResultsPage] no results available")
              Redirect(routes.ErrorController.renderNoResultAvailablePage)
          }
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][renderUploadResultsPage] user not authorised")
          resp
      }
  }

  def getResultsFile(fileName:String):  Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          userCanGetFile(userId, fileName).flatMap {
            case true =>
              shortLivedCache.removeFileSessionFromCache(userId)
              getFile(fileName)
            case false =>
              Future.successful(Redirect(routes.ErrorController.fileNotAvailable))
          }
        case Left(resp) =>
          Logger.error("[WhatDoYouWantToDoController][getResultsFile] user not authorised")
          resp
      }
  }

  private def userCanGetFile(userId: String, fileName: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    shortLivedCache.fetchFileSession(userId).map {
      case Some(fileSession) =>
        fileSession.resultsFile match {
          case Some(resultFile) =>
            resultFile.filename match {
              case Some(name) if name == fileName =>
                true
              case _ =>
                Logger.error("[WhatDoYouWantToDoController][getResultsFile] filename empty")
                false
            }
          case _ =>
            Logger.error("[WhatDoYouWantToDoController][getResultsFile] no result file found")
            false
        }
      case _ =>
        Logger.error("[WhatDoYouWantToDoController][getResultsFile] no file session for User Id")
        false
    }
  }

  private def getFile(fileName: String)(implicit hc: HeaderCarrier): Future[play.api.mvc.Result] = {
    resultsFileConnector.getFile(fileName).map { response =>
      val dataContent: Source[ByteString, _] = StreamConverters.fromInputStream(() => response.get)

      Ok.sendEntity(HttpEntity.Streamed(dataContent, None, Some(_contentType)))
        .withHeaders(CONTENT_DISPOSITION -> s"""attachment; filename="${fileName}.csv"""",
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
