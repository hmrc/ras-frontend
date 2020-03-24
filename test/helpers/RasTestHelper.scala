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

package helpers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import config.{ApplicationConfig, RasSessionCache, RasShortLivedHttpCaching}
import connectors.{FileUploadConnector, ResidencyStatusAPIConnector, UserDetailsConnector}
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import services.{AuditService, SessionService, ShortLivedCache}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.test.WithFakeApplication
import org.mockito.Mockito._

trait RasTestHelper extends MockitoSugar with WithFakeApplication {  this: Suite =>
	implicit val actorSystem: ActorSystem = ActorSystem()
	implicit val materializer: ActorMaterializer = ActorMaterializer()


	val SCOTTISH = "scotResident"
	val WELSH = "welshResident"
	val OTHER_UK = "otherUKResident"

	val fakeRequest = FakeRequest()
	val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
	val mockAppCrypto: ApplicationCrypto = mock[ApplicationCrypto]

	val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
	val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
	val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
	val mockAuditService: AuditService = mock[AuditService]

	val mockFileUploadConnector: FileUploadConnector = mock[FileUploadConnector]
	val mockResidencyStatusAPIConnector: ResidencyStatusAPIConnector = mock[ResidencyStatusAPIConnector]
	val mockUserDetailsConnector: UserDetailsConnector = mock[UserDetailsConnector]
	val mockSessionService: SessionService = mock[SessionService]
	val mockRasSessionCache: RasSessionCache = mock[RasSessionCache]
	val mockRasShortLivedHttpCache : RasShortLivedHttpCaching = mock[RasShortLivedHttpCaching]
	val mockShortLivedCache: ShortLivedCache = mock[ShortLivedCache]

	when(mockAppConfig.hoursToWaitForReUpload).thenReturn(24)
	when(mockAppConfig.reportAProblemPartialUrl).thenReturn("reportAProblemPartialUrl")
	when(mockAppConfig.reportAProblemNonJSUrl).thenReturn("reportAProblemNonJSUrl")
	when(mockAppConfig.timeOutSeconds).thenReturn(780)
	when(mockAppConfig.timeOutCountDownSeconds).thenReturn(120)
	when(mockAppConfig.signOutUrl).thenReturn("signOutUrl")
	when(mockAppConfig.signOutAndContinueUrl).thenReturn("signOutAndContinueUrl")
	when(mockAppConfig.analyticsToken).thenReturn("analyticsToken")
	when(mockAppConfig.analyticsHost).thenReturn("analyticsHost")
	when(mockAppConfig.urBannerEnabled).thenReturn(false)
	when(mockAppConfig.urBannerLinkUrl).thenReturn("urBannerLinkUrl")
	when(mockAppConfig.rasFrontendBaseUrl).thenReturn("http://localhost:9673")
	when(mockAppConfig.rasFrontendUrlSuffix).thenReturn("relief-at-source")
	when(mockAppConfig.fileUploadFrontendBaseUrl).thenReturn("http://localhost:8899")
	when(mockAppConfig.fileUploadFrontendSuffix).thenReturn("file-upload/upload/envelopes")
}
