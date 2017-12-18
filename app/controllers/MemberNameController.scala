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
import forms.MemberNameForm._
import play.api.mvc.Action
import play.api.{Configuration, Environment, Logger, Play}
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

object MemberNameController extends MemberNameController {
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
}

trait MemberNameController extends RasController with PageFlowController {

  implicit val context: RasContext = RasContextImpl

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          Logger.debug("[NameController][get] user authorised")
          sessionService.fetchRasSession() map {
            case Some(session) => Ok(views.html.member_name(form.fill(session.name)))
            case _ => Ok(views.html.member_name(form))
          }
        case Left(resp) =>
          Logger.debug("[NameController][get] user Not authorised")
          resp
      }
  }

  def post = Action.async { implicit request =>
    isAuthorised.flatMap{
      case Right(_) =>
      form.bindFromRequest.fold(
        formWithErrors => {
          Logger.debug("[NameController][post] Invalid form field passed")
          Future.successful(BadRequest(views.html.member_name(formWithErrors)))
        },
        memberName => {
          Logger.debug("[NameController][post] valid form")
          sessionService.cacheName(memberName) flatMap {
            case Some(session) => Future.successful(Redirect(routes.MemberNinoController.get()))
            case _ => Future.successful(Redirect(routes.GlobalErrorController.get))
          }
        }
      )
      case Left(res) => res
    }
  }

  def back = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userInfo) => Future.successful(previousPage("MemberNameController"))
        case Left(res) => res
      }
  }

}

