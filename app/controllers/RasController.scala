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

import config.{ApplicationConfig, RasContext, RasContextImpl}
import connectors.UserDetailsConnector
import helpers.helpers.I18nHelper
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import services.SessionService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core._


import scala.concurrent.Future
import uk.gov.hmrc.play.frontend.config.AuthRedirects

trait RasController extends FrontendController with I18nHelper with AuthorisedFunctions with AuthRedirects {

  val userDetailsConnector:UserDetailsConnector
  val sessionService: SessionService = SessionService

  def isAuthorised()(implicit request: Request[AnyContent]) = {
    authorised(AuthProviders(GovernmentGateway) and (Enrolment("HMRC-PSA-ORG") or Enrolment("HMRC-PP-ORG"))).retrieve(internalId and userDetailsUri)
    {case (id ~ uri) =>
      Logger.warn("User authorised");

      val userId = id.getOrElse(Left(userInfoNotFond("userId")))
      val userUri = uri.getOrElse(throw new RuntimeException("No userDetailsUri for user"))

      userDetailsConnector.getUserDetails(userUri)(hc) map{Right(_)}
    } recover {
      case _ : NoActiveSession => Left(notLogged)
      case _ : AuthorisationException => Left(unAuthorise)
    }
  }

  def notLogged() = {Logger.warn("User not logged in - no active session found");Future.successful(toGGLogin(ApplicationConfig.loginCallback))}

  def unAuthorise() = {
    Logger.warn("User not authorised");
    Future.successful(Redirect(routes.GlobalErrorController.notAuthorised))
  }

  def userInfoNotFond(idName:String) = {
    Logger.warn(s"${idName} not found");
    Future.successful(Redirect(routes.GlobalErrorController.get()))
  }

}


