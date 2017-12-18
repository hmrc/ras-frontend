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

import config.{FrontendAuthConnector, RasContext, RasContextImpl}
import connectors.UserDetailsConnector
import forms.MemberNinoForm.form
import play.api.mvc.Action
import play.api.{Configuration, Environment, Logger, Play}
import uk.gov.hmrc.auth.core.AuthConnector


import scala.concurrent.Future

object MemberNinoController extends MemberNinoController {
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
}

trait MemberNinoController extends RasController with PageFlowController{

  implicit val context: RasContext = RasContextImpl

  var firstName = ""

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Logger.debug("[NinoController][get] user authorised")
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              Ok(views.html.member_nino(form.fill(session.nino),name))
            case _ =>
              Ok(views.html.member_nino(form, Messages("member")))
          }
        case Left(resp) =>
          Logger.debug("[NinoController][get] user Not authorised")
          resp
      }
  }

  def post = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          form.bindFromRequest.fold(
            formWithErrors => {
              Logger.debug("[NinoController][post] Invalid form field passed")
              Future.successful(BadRequest(views.html.member_nino(formWithErrors,firstName)))
            },
            nino => {
              Logger.debug("[NinoController][post] valid form")
              sessionService.cacheNino(nino) flatMap {
                case Some(session) => Future.successful(Redirect(routes.MemberDOBController.get()))
                case _ => Future.successful(Redirect(routes.GlobalErrorController.get))
              }
            }
          )
        case Left(resp) =>
          Logger.debug("[NinoController][post] user Not authorised")
          resp
      }
  }

  def back = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userInfo) =>
          sessionService.fetchRasSession() map {
            case Some(session) => previousPage("MemberNinoController")
            case _ => Redirect(routes.GlobalErrorController.get)
          }
        case Left(res) => res
      }
  }

}
