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
import play.api.Logging
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import play.api.mvc.Results._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait RasController extends AuthorisedFunctions with Logging {

	val appConfig: ApplicationConfig

	def isAuthorised()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Future[Result], String]] = {
    authorised(AuthProviders(GovernmentGateway) and (Enrolment("HMRC-PSA-ORG") or Enrolment("HMRC-PP-ORG"))
    ).retrieve(authorisedEnrolments) {
			enrolments =>
				Future(Right(enrolments.enrolments.head.identifiers.head.value))
		} recover {
      case e: NoActiveSession => Left(notLogged(e))
      case ex : AuthorisationException => Left(unAuthorise(ex))
    }
  }

  def notLogged(e: NoActiveSession): Future[Result] = {
    logger.warn(s"[RasController][notLogged] No Active Session - $e")
    Future.successful(toGGLogin(appConfig.loginCallback))
  }

  def unAuthorise(ex: AuthorisationException): Future[Result] = {
    logger.warn(s"[RasController][unAuthorise] User not authorised - $ex")
    Future.successful(Redirect(routes.ErrorController.notAuthorised()))
  }

	def toGGLogin(continueUrl: String): Result = {
		Redirect(
      appConfig.loginURL,
			Map(
				"continue_url" -> Seq(continueUrl),
				"origin"   -> Seq("ras-frontend")
			)
		)
	}

}
