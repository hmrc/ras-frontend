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
import forms.{MemberNinoForm => form}
import javax.inject.Inject
import models.ApiVersion
import play.api.Logger
import play.api.mvc.{Action, AnyContent}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.Future

class MemberNinoController @Inject()(val authConnector: DefaultAuthConnector,
                                     val residencyStatusAPIConnector: ResidencyStatusAPIConnector,
                                     val connector: DefaultAuditConnector,
                                     val shortLivedCache: ShortLivedCache,
                                     val sessionService: SessionService,
                                     implicit val appConfig: ApplicationConfig
                                    ) extends RasResidencyCheckerController with PageFlowController {

  lazy val apiVersion: ApiVersion = appConfig.rasApiVersion

  def getFullName()(implicit hc: HeaderCarrier): Future[String] = {
    sessionService.fetchRasSession() map {
      case Some(session) => session.name.firstName.capitalize + " " + session.name.lastName.capitalize
      case _ => Messages("member")
    }
  }

  def get(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(session) =>
              val name = session.name.firstName.capitalize + " " + session.name.lastName.capitalize
              Ok(views.html.member_nino(form(Some(name)).fill(session.nino), name, edit))
            case _ =>
              Ok(views.html.member_nino(form(), Messages("member"), edit))
          }
        case Left(resp) =>
          Logger.warn("[NinoController][get] user Not authorised")
          resp
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          getFullName() flatMap { name =>
            form(Some(name)).bindFromRequest.fold(
              formWithErrors => {
                Logger.warn("[NinoController][post] Invalid form field passed")
                Future.successful(BadRequest(views.html.member_nino(formWithErrors, name, edit)))
              },
              memberNino => {
                sessionService.cacheNino(memberNino.copy(nino = memberNino.nino.replaceAll("\\s", ""))) flatMap {
                  case Some(session) =>
                    if (edit) {
                      submitResidencyStatus(session, userId)
                    } else {
                      Future.successful(Redirect(routes.MemberDOBController.get()))
                    }
                  case _ => Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage()))
                }
              }
            )
          }
        case Left(resp)
        =>
          Logger.warn("[NinoController][post] user Not authorised")
          resp
      }
  }

  def back(edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(_) =>
          sessionService.fetchRasSession() map {
            case Some(_) => previousPage("MemberNinoController", edit)
            case _ => Redirect(routes.ErrorController.renderGlobalErrorPage())
          }
        case Left(res) =>
          Logger.warn("[NinoController][back] user Not authorised")
          res
      }
  }
}
