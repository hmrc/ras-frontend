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
import connectors.ResidencyStatusAPIConnector
import forms.MemberDateOfBirthForm.form
import javax.inject.Inject
import models.ApiVersion
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{Action, AnyContent}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.Future

class MemberDOBController @Inject()(val authConnector: DefaultAuthConnector,
																		val residencyStatusAPIConnector: ResidencyStatusAPIConnector,
																		val connector: DefaultAuditConnector,
																		val shortLivedCache: ShortLivedCache,
																		val sessionService: SessionService,
																		implicit val appConfig: ApplicationConfig
																	 ) extends RasResidencyCheckerController with PageFlowController {

	lazy val apiVersion: ApiVersion = appConfig.rasApiVersion

	def get(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              Ok(views.html.member_dob(form.fill(session.dateOfBirth),name, edit))
            case _ => Ok(views.html.member_dob(form, "", edit))
          }
        case Left(resp) =>
          Logger.warn("[DobController][get] user Not authorised")
          resp
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          form.bindFromRequest.fold(
            formWithErrors => {
              Logger.warn("[DobController][post] Invalid form field passed")
              sessionService.fetchRasSession() map {
                case Some(session) =>
                  implicit val formInstance: Option[Form[models.MemberDateOfBirth]] = Some(formWithErrors)
                  val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
                  BadRequest(views.html.member_dob(formWithErrors, name, edit))
                case _ =>
                  BadRequest(views.html.member_dob(formWithErrors, Messages("member"), edit))
              }
            },
            dateOfBirth => {
              sessionService.cacheDob(dateOfBirth) flatMap {
                case Some(session) => submitResidencyStatus(session, userId)
                case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
              }
            }
          )
        case Left(res) =>
          Logger.warn("[DobController][back] user Not authorised")
          res
      }
  }

  def back(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(_) => previousPage("MemberDOBController", edit)
            case _ => Redirect(routes.ErrorController.renderGlobalErrorPage())
          }
        case Left(res) =>
          Logger.warn("[DobController][back] user Not authorised")
          res
      }
  }
}
