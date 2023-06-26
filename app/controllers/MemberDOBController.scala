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
import connectors.ResidencyStatusAPIConnector
import forms.{MemberDateOfBirthForm => form}
import models.ApiVersion
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.WithUnsafeDefaultFormBinding
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import validators.DateValidator

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MemberDOBController @Inject()(val authConnector: DefaultAuthConnector,
																		val residencyStatusAPIConnector: ResidencyStatusAPIConnector,
																		val connector: DefaultAuditConnector,
																		val shortLivedCache: ShortLivedCache,
																		val sessionService: SessionService,
																		val mcc: MessagesControllerComponents,
																		implicit val appConfig: ApplicationConfig,
                                    memberDobView: views.html.member_dob
																	 ) extends FrontendController(mcc) with RasResidencyCheckerController with PageFlowController with Logging with DateValidator with WithUnsafeDefaultFormBinding {

	implicit val ec: ExecutionContext = mcc.executionContext
	lazy val apiVersion: ApiVersion = appConfig.rasApiVersion

	def get(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              Ok(memberDobView(form(Some(name)).fill(session.dateOfBirth), name, edit))
            case _ => Ok(memberDobView(form(), "member", edit))
          }
        case Left(resp) =>
          logger.warn("[DobController][get] user Not authorised")
          resp
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(userId) =>
          getFullName() flatMap { name =>
            form(Some(name)).bindFromRequest().fold(
              formWithErrors => {
                logger.warn("[DobController][post] Invalid form field passed")
                val updatedFormWithErrors = updatedErrors(formWithErrors)
                Future.successful(BadRequest(memberDobView(updatedFormWithErrors, name, edit)))
              },
              dateOfBirth => {
                sessionService.cacheDob(dateOfBirth) flatMap {
                  case Some(session) => submitResidencyStatus(session, userId)
                  case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
                }
              }
            )
          }
        case Left(res) =>
          logger.warn("[DobController][back] user Not authorised")
          res
      }
  }

  def back(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(_) => previousPage("MemberDOBController", edit)
            case _ => Redirect(routes.ErrorController.renderGlobalErrorPage)
          }
        case Left(res) =>
          logger.warn("[DobController][back] user Not authorised")
          res
      }
  }
}
