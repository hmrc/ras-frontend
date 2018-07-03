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
import connectors.UserDetailsConnector
import play.api.mvc.Action
import play.api.{Configuration, Environment, Logger, Play}
import uk.gov.hmrc.auth.core.AuthConnector
import services.CacheKeys

import scala.concurrent.Future

object UrDismissalController extends UrDismissalController {
  // $COVERAGE-OFF$Disabling highlighting by default until a workaround for https://issues.scala-lang.org/browse/SI-8596 is found
  val authConnector: AuthConnector = FrontendAuthConnector
  override val userDetailsConnector: UserDetailsConnector = UserDetailsConnector
  val config: Configuration = Play.current.configuration
  val env: Environment = Environment(Play.current.path, Play.current.classloader, Play.current.mode)
  // $COVERAGE-ON$
}

trait UrDismissalController extends RasController {

  implicit val context: RasContext = RasContextImpl

  def dismissUrBanner = Action.async {
    implicit request =>
      isAuthorised.flatMap {
        case Right(userId) =>
          sessionService.cache(CacheKeys.UrBannerDismissed, Some(true))
          Future.successful(NoContent)
        case Left(resp) =>
          Logger.error("[UrDismissalController][dismissUrBanner] user not authorised")
          resp
      }
  }
}
