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

package config

import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val signOutUrl: String
  val betaFeedbackUrl: String
  val betaFeedbackUnauthenticatedUrl: String
  val loginCallback:String
  val fileUploadCallBack: String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = configuration.getString(s"contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "RAS"
  private val caFrontendHost = configuration.getString("ca-frontend.host").getOrElse("")

  override lazy val analyticsToken = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost = loadConfig(s"google-analytics.host")

  private val logoutCallback = configuration.getString("gg-urls.logout-callback.url").getOrElse("/lifetime-isa")

  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val signOutUrl = s"$caFrontendHost/gg/sign-out?continue=$logoutCallback"
  override lazy val betaFeedbackUrl: String = s"$contactHost/contact/beta-feedback"
  override lazy val betaFeedbackUnauthenticatedUrl: String = s"$contactHost/contact/beta-feedback-unauthenticated"
  override lazy val loginCallback: String = configuration.getString("gg-urls.login-callback.url").getOrElse("/lifetime-isa")
  override lazy val fileUploadCallBack: String = configuration.getString("file-upload-ras-callback-url")
    .getOrElse(throw new Exception("Missing configuration key: file-upload-ras-callback-url"))
}
