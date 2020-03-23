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
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent}
import services.{SessionService, ShortLivedCache, TaxYearResolver}
import uk.gov.hmrc.auth.core.AuthConnector

class ResultsController @Inject()(val authConnector: AuthConnector,
																	val shortLivedCache: ShortLivedCache,
																	val sessionService: SessionService,
																	implicit val appConfig: ApplicationConfig
																 ) extends PageFlowController {

def matchFound: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
						case Some(session) =>
							session.residencyStatusResult match {
								case Some(residencyStatusResult) =>
									val name = s"${session.name.firstName.capitalize} ${session.name.lastName.capitalize}"
									val dateOfBirth = session.dateOfBirth.dateOfBirth.asLocalDate.toString("d MMMM yyyy")
									val nino = session.nino.nino
									val currentTaxYear = TaxYearResolver.currentTaxYear
									val nextTaxYear = TaxYearResolver.currentTaxYear + 1
									val currentYearResidencyStatus = residencyStatusResult.currentYearResidencyStatus
									val nextYearResidencyStatus = residencyStatusResult.nextYearResidencyStatus

									sessionService.resetRasSession()

									Logger.info("[ResultsController][matchFound] Successfully retrieved ras session")
									Ok(views.html.match_found(
										name, dateOfBirth, nino,
										currentYearResidencyStatus,
										nextYearResidencyStatus,
										currentTaxYear, nextTaxYear))

								case _ =>
									Logger.info("[ResultsController][matchFound] Session does not contain residency status result - wrong result")
									Redirect(routes.ChooseAnOptionController.get())
							}
						case _ =>
							Logger.error("[ResultsController][matchFound] failed to retrieve ras session")
							Redirect(routes.ErrorController.renderGlobalErrorPage())
					}
        case Left(res) =>
          Logger.warn("[ResultsController][matchFound] user Not authorised")
          res
      }
  }

  def noMatchFound: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
						case Some(session) =>
							if (session.name.hasAValue() && session.nino.hasAValue() && session.dateOfBirth.hasAValue()) {
								session.residencyStatusResult match {
									case None =>

										val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
										val nino = session.nino.nino
										val dateOfBirth = session.dateOfBirth.dateOfBirth.asLocalDate.toString("d MMMM yyyy")

										Logger.info("[ResultsController][noMatchFound] Successfully retrieved ras session")
										Ok(views.html.match_not_found(name, dateOfBirth, nino))

									case Some(_) =>
										Logger.info("[ResultsController][noMatchFound] Session contains residency result - wrong result")
										Redirect(routes.ChooseAnOptionController.get())
								}
							} else {
								Logger.info("[ResultsController][noMatchFound] Session does not contain residency status result")
								Redirect(routes.ChooseAnOptionController.get())
							}
						case _ =>
							Logger.error("[ResultsController][noMatchFound] failed to retrieve ras session")
							Redirect(routes.ErrorController.renderGlobalErrorPage())
					}
        case Left(res) =>
          Logger.warn("[ResultsController][matchFound] user Not authorised")
          res
      }
  }

  def back: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(_) => previousPage("ResultsController")
            case _ => Redirect(routes.ErrorController.renderGlobalErrorPage())
          }
        case Left(res) =>
          Logger.warn("[ResultsController][back] user Not authorised")
          res
      }
  }
}
