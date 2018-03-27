/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.{ResidencyStatusAPIConnector, UserDetailsConnector}
import play.api.mvc.Action
import play.api.{Configuration, Environment, Logger, Play}
import uk.gov.hmrc.auth.core.AuthConnector
import forms.MemberDateOfBirthForm.form
import metrics.Metrics
import services.AuditService

import scala.concurrent.Future

object MemberDOBController extends MemberDOBController {
  // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  override val residencyStatusAPIConnector = ResidencyStatusAPIConnector
  override val auditService: AuditService = AuditService
  // $COVERAGE-ON$
}

trait MemberDOBController extends RasResidencyCheckerController with PageFlowController {

  implicit val context: RasContext = RasContextImpl

  def get = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              Ok(views.html.member_dob(form.fill(session.dateOfBirth), name))
            case _ => Ok(views.html.member_dob(form))
          }
        case Left(resp) =>
          Logger.error("[DobController][get] user Not authorised")
          resp
      }
  }

  def post = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          form.bindFromRequest.fold(
            formWithErrors => {
              Logger.error("[DobController][post] Invalid form field passed")
              Future.successful(BadRequest(views.html.member_dob(formWithErrors)))
            },
            dateOfBirth => {
              sessionService.cacheDob(dateOfBirth) flatMap {
                case Some(session) => submitResidencyStatus(session, dateOfBirth, userId)
                case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
              }
            }
          )
        case Left(res) => res
      }
  }

  def back = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userInfo) =>
          sessionService.fetchRasSession() map {
            case Some(session) => previousPage("MemberDOBController")
            case _ => Redirect(routes.ErrorController.renderGlobalErrorPage())
          }
        case Left(res) => res
      }
  }
}