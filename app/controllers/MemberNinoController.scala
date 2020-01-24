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
import forms.MemberNinoForm.form
import models.ApiVersion
import play.api.mvc.Action
import play.api.{Configuration, Environment, Logger, Play}
import services.AuditService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

object MemberNinoController extends MemberNinoController {
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  override val residencyStatusAPIConnector = ResidencyStatusAPIConnector
  override val auditService: AuditService = AuditService
  override lazy val apiVersion: ApiVersion = ApplicationConfig.rasApiVersion
}

trait MemberNinoController extends RasResidencyCheckerController with PageFlowController {

  implicit val context: RasContext = RasContextImpl

  def get(edit: Boolean = false) = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              Ok(views.html.member_nino(form.fill(session.nino), name, edit))
            case _ =>
              Ok(views.html.member_nino(form, Messages("member"), edit))
          }
        case Left(resp) =>
          Logger.error("[NinoController][get] user Not authorised")
          resp
      }
  }

  def post(edit: Boolean = false) = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          form.bindFromRequest.fold(
            formWithErrors => {
              Logger.error("[NinoController][post] Invalid form field passed")
              sessionService.fetchRasSession() map {
                case Some(session) =>
                  val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
                  BadRequest(views.html.member_nino(formWithErrors, name, edit))
                case _ =>
                  BadRequest(views.html.member_nino(formWithErrors, Messages("member"), edit))
              }
            },
            memberNino => {
              sessionService.cacheNino(memberNino.copy(nino = memberNino.nino.replaceAll("\\s", ""))) flatMap {
                case Some(session) => {
                  edit match {
                    case true => submitResidencyStatus(session, userId)
                    case _ => Future.successful(Redirect(routes.MemberDOBController.get()))
                  }
                }
                case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
              }
            }
          )
        case Left(resp) =>
          Logger.error("[NinoController][post] user Not authorised")
          resp
      }
  }

  def back(edit: Boolean = false) = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userInfo) =>
          sessionService.fetchRasSession() map {
            case Some(session) => previousPage("MemberNinoController", edit)
            case _ => Redirect(routes.ErrorController.renderGlobalErrorPage())
          }
        case Left(res) => res
      }
  }
}
