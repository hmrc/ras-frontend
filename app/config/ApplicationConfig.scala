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

package config

import javax.inject.Inject
import models.{ApiV1_0, ApiV2_0, ApiVersion}
import play.api.{Configuration, Play}
import play.api.Mode.Mode
import uk.gov.hmrc.play.config.ServicesConfig


//TODO Inject services config rather than extend during play 2.6 upgrade
class ApplicationConfig @Inject()() extends ServicesConfig {

	override protected def mode: Mode = Play.current.mode
	override protected def runModeConfiguration: Configuration = Play.current.configuration

  private def loadConfig(key: String) = getString(key)

	lazy val contactHost: String = loadConfig("contact-frontend.host")
  private lazy val contactFormServiceIdentifier: String = "RAS"
	lazy val caFrontendHost: String = loadConfig("ca-frontend.host")

	lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
	lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
	lazy val hoursToWaitForReUpload: Int = getConfInt(s"re-upload.wait.time.hours", 24)

  private lazy val logoutCallback: String = getConfString("gg-urls.logout-callback.url", "/relief-at-source/")
  private lazy val signOutBaseUrl: String = s"$caFrontendHost/gg/sign-out?continue="
  private lazy val continueCallback =  getConfString("gg-urls.continue-callback.url", "/relief-at-source/")

	lazy val companyAuthHost: String = s"${getConfString("auth.company-auth.host", "")}"
	lazy val loginURL: String = s"$companyAuthHost/gg/signin"

	lazy val reportAProblemUrl: String = s"$contactHost/contact/problem_reports"
	lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
	lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
	lazy val signOutUrl: String = s"$signOutBaseUrl$logoutCallback"
	lazy val signOutAndContinueUrl: String = s"$signOutBaseUrl$continueCallback"
	lazy val loginCallback: String = getConfString("gg-urls.login-callback.url","/relief-at-source/")
	lazy val fileUploadCallBack: String = loadConfig("file-upload-ras-callback-url")
	lazy val fileDeletionUrl: String = getConfString("file-deletion-url","/ras-api/file/remove/")
	lazy val rasApiResidencyStatusEndpoint: String = loadConfig("residency-status-url")

	lazy val urBannerEnabled: Boolean = getConfBool("ur-banner.enabled", defBool = false)
	lazy val urBannerLinkUrl: String = getConfString("ur-banner.link-url","")

	lazy val rasApiVersion: ApiVersion = loadConfig("ras-api-version") match {
    case "1.0" => ApiV1_0
    case "2.0" => ApiV2_0
    case _ => throw new Exception(s"Invalid value for configuration key: ras-api-version")
  }

	lazy val timeOutSeconds : Int = getConfInt("sessionTimeout.timeoutSeconds",780)
	lazy val timeOutCountDownSeconds: Int = getConfInt("sessionTimeout.time-out-countdown-seconds",120)
	lazy val refreshInterval: Int = timeOutSeconds + 10
	lazy val enableRefresh: Boolean= getConfBool("sessionTimeout.enableRefresh", defBool = true)


	//FileUpload
	lazy val rasApiBaseUrl: String = baseUrl("relief-at-source")
	lazy val fileUploadBaseUrl: String = baseUrl("file-upload")
	lazy val fileUploadUrlSuffix: String = loadConfig("file-upload-url-suffix")
	lazy val maxItems: Int = getInt("file-upload-constraints.maxItems")
	lazy val maxSize: String = loadConfig("file-upload-constraints.maxSize")
	lazy val maxSizePerItem: String = loadConfig("file-upload-constraints.maxSizePerItem")
	lazy val allowZeroLengthFiles: Boolean = getBoolean("file-upload-constraints.allowZeroLengthFiles")
	lazy val rasFrontendBaseUrl: String = loadConfig("ras-frontend.host")
	lazy val rasFrontendUrlSuffix: String = loadConfig("ras-frontend-url-suffix")
	lazy val fileUploadFrontendBaseUrl: String = loadConfig("file-upload-frontend.host")
	lazy val fileUploadFrontendSuffix: String = loadConfig("file-upload-frontend-url-suffix")

	//SessionCacheWiring
	lazy val shortLivedCacheBaseUri: String = baseUrl("cachable.short-lived-cache")
	lazy val shortLivedCacheDomain: String = getString(s"$rootServices.cachable.short-lived-cache.domain")
	lazy val sessionCacheBaseUri: String = baseUrl("keystore")
	lazy val sessionCacheDomain: String = getString(s"$rootServices.cachable.session-cache.domain")

}
