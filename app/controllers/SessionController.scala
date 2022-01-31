/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class SessionController @Inject()(val authConnector: DefaultAuthConnector,
																	val shortLivedCache: ShortLivedCache,
																	val sessionService: SessionService,
																	val mcc: MessagesControllerComponents,
																	val appConfig: ApplicationConfig
																 ) extends FrontendController(mcc) with RasController with Logging {

	implicit val ec: ExecutionContext = mcc.executionContext

	val CHOOSE_AN_OPTION = "choose-an-option"
  val MEMBER_NAME = "member-name"
  val MEMBER_NINO = "member-nino"
  val MEMBER_DOB = "member-dob"

  def redirect(target:String, cleanSession:Boolean, edit: Boolean = false): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(_) =>
          if (cleanSession) {
            sessionService.resetRasSession() map {
              case Some(_) =>
                target match {
                  case CHOOSE_AN_OPTION => Redirect(routes.ChooseAnOptionController.get)
                  case MEMBER_NAME => Redirect(routes.MemberNameController.get(edit))
                  case MEMBER_NINO => Redirect(routes.MemberNinoController.get(edit))
                  case MEMBER_DOB => Redirect(routes.MemberDOBController.get(edit))
                  case _ =>
                    logger.error(s"[SessionController][redirect] Invalid redirect target $target")
                    Redirect(routes.ErrorController.renderGlobalErrorPage)
                }
              case _ =>
                logger.error("[SessionController][redirect] No session found")
                Redirect(routes.ErrorController.renderGlobalErrorPage)
            }
          } else {
            target match {
              case CHOOSE_AN_OPTION => Future.successful(Redirect(routes.ChooseAnOptionController.get))
              case MEMBER_NAME => Future.successful(Redirect(routes.MemberNameController.get(edit)))
              case MEMBER_NINO => Future.successful(Redirect(routes.MemberNinoController.get(edit)))
              case MEMBER_DOB => Future.successful(Redirect(routes.MemberDOBController.get(edit)))
              case _ =>
                logger.error(s"[SessionController][cleanAndRedirect] Invalid redirect target $target")
                Future.successful(Redirect(routes.ErrorController.renderGlobalErrorPage))
            }
          }
        case Left(resp) =>
          logger.warn("[SessionController][redirect] User is unauthenticated")
          resp
      }
  }
  def keepAlive(): Action[AnyContent] = Action.async {
     Future.successful(Ok("OK"))
  }
}
