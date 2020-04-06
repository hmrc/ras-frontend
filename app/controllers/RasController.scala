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

import config.ApplicationConfig
import helpers.I18nHelper
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Result}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

trait RasController extends FrontendController with I18nHelper with AuthorisedFunctions {

	val appConfig: ApplicationConfig
  val sessionService: SessionService
  val shortLivedCache: ShortLivedCache

  def isAuthorised()(implicit request: Request[AnyContent]): Future[Either[Future[Result], String]] = {
    authorised(AuthProviders(GovernmentGateway) and (Enrolment("HMRC-PSA-ORG") or Enrolment("HMRC-PP-ORG"))
    ).retrieve(authorisedEnrolments) {
			enrolments =>
				Logger.info("User authorised")
				Future(Right(enrolments.enrolments.head.identifiers.head.value))
		} recover {
      case _ : NoActiveSession => Left(notLogged())
      case ex : AuthorisationException => Left(unAuthorise(ex))
    }
  }

  def notLogged(): Future[Result] = {
    Logger.warn("[RasController][notLogged] User not logged in - no active session found")
    Future.successful(toGGLogin(appConfig.loginCallback))
  }

  def unAuthorise(ex: AuthorisationException): Future[Result] = {
    Logger.error(s"[RasController][unAuthorise] User not authorised - ${ex.reason}")
    Future.successful(Redirect(routes.ErrorController.notAuthorised()))
  }

  def userInfoNotFond(idName:String): Future[Result] = {
    Logger.error(s"[RasController][userInfoNotFound] $idName not found")
    Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
  }

	def toGGLogin(continueUrl: String): Result = {
		Redirect(
      appConfig.loginURL,
			Map(
				"continue" -> Seq(continueUrl),
				"origin"   -> Seq("ras-frontend")
			)
		)
	}

}
