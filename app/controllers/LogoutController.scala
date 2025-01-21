/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LogoutController @Inject()(val authConnector: DefaultAuthConnector,
                                 val mcc: MessagesControllerComponents,
                                 val appConfig: ApplicationConfig
                                ) extends FrontendController(mcc) with RasController with Logging {

  implicit val ec: ExecutionContext = mcc.executionContext

  def logout: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised().flatMap {
        case Right(_) => Future.successful(
          Redirect(appConfig.feedbackUrl).withNewSession
        )
        case Left(resp) =>
          logger.warn("[LogoutController][logout] User is unauthenticated")
          resp
      }
  }

}
