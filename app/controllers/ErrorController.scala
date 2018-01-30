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

import config.RasContextImpl
import helpers.helpers.I18nHelper
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ErrorController extends ErrorController

trait ErrorController extends FrontendController with I18nHelper {

  implicit val context: config.RasContext = RasContextImpl

  def renderGlobalErrorPage = Action.async {
    implicit request =>
      Future.successful(InternalServerError(views.html.global_error()))
  }

  def renderProblemGettingResultsPage = Action.async {
    implicit request =>
      Future.successful(InternalServerError(views.html.problem_getting_results()))
  }

  def renderProblemUploadingFilePage = Action.async {
    implicit request =>
      Future.successful(InternalServerError(views.html.problem_uploading_file()))
  }

  def notAuthorised = Action.async {
    implicit request =>
      Future.successful(Ok(views.html.unauthorised()))
  }
}
