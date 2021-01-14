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

import config.ApplicationConfig
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class ErrorController @Inject()(val authConnector: DefaultAuthConnector,
																val shortLivedCache: ShortLivedCache,
																val sessionService: SessionService,
																val mcc: MessagesControllerComponents,
																implicit val appConfig: ApplicationConfig) extends FrontendController(mcc) with RasController {

	implicit val ec: ExecutionContext = mcc.executionContext

	def renderGlobalErrorPage: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Logger.info("[ErrorController][renderGlobalErrorPage] rendering global error page")
          Future.successful(InternalServerError(views.html.global_error()))
        case Left(resp) =>
          Logger.warn("[ErrorController][renderGlobalErrorPage] user not authorised")
          resp
      }
  }

  def renderProblemUploadingFilePage: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Logger.info("[ErrorController][renderProblemUploadingFilePage] rendering problem uploading file page")
          Future.successful(InternalServerError(views.html.problem_uploading_file()))
        case Left(resp) =>
          Logger.warn("[ErrorController][renderProblemUploadingFilePage] user not authorised")
          resp
      }
  }

  def fileNotAvailable: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Logger.info("[ErrorController][fileNotAvailable] rendering file not available page")
          Future.successful(InternalServerError(views.html.file_not_available()))
        case Left(resp) =>
          Logger.warn("[ErrorController][fileNotAvailable] user not authorised")
          resp
      }
  }

  def notAuthorised: Action[AnyContent] = Action.async {
    implicit request =>
      Future.successful(Ok(views.html.unauthorised()))
  }
}
