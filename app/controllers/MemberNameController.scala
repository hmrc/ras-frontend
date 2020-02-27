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

import config.{ApplicationConfig, FrontendAuthConnector, RasContext, RasContextImpl}
import connectors.{ResidencyStatusAPIConnector, UserDetailsConnector}
import forms.MemberNameForm._
import models.ApiVersion
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment, Logger, Play}
import services.AuditService
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

object MemberNameController extends MemberNameController {
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  override val residencyStatusAPIConnector = ResidencyStatusAPIConnector
  override val auditService: AuditService = AuditService
  override lazy val apiVersion: ApiVersion = ApplicationConfig.rasApiVersion
}

trait MemberNameController extends RasResidencyCheckerController with PageFlowController {

  implicit val context: RasContext = RasContextImpl

  def get(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(session) => Ok(views.html.member_name(form.fill(session.name), edit))
            case _ => Ok(views.html.member_name(form, edit))
          }
        case Left(resp) =>
          Logger.warn("[NameController][get] user Not authorised")
          resp
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = Action.async { implicit request =>
    isAuthorised.flatMap{
      case Right(userId) =>
      form.bindFromRequest.fold(
        formWithErrors => {
          Logger.warn("[NameController][post] Invalid form field passed")
          Future.successful(BadRequest(views.html.member_name(formWithErrors, edit)))
        },
        memberName => {
          sessionService.cacheName(memberName) flatMap {
            case Some(session) => {
              edit match {
                case true => submitResidencyStatus(session, userId)
                case _ => Future.successful(Redirect(routes.MemberNinoController.get()))
              }
            }
            case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
          }
        }
      )
      case Left(res) =>
        Logger.warn("[NameController][post] user Not authorised")
        res
    }
  }

  def back(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) => Future.successful(previousPage("MemberNameController", edit))
        case Left(res) =>
          Logger.warn("[NameController][back] user Not authorised")
          res
      }
  }

}

