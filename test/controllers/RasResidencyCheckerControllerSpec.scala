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

import connectors.{ResidencyStatusAPIConnector, UserDetailsConnector}
import controllers.RasResidencyCheckerController._
import helpers.helpers.I18nHelper
import models._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import services.{AuditService, SessionService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec

class RasResidencyCheckerControllerSpec extends UnitSpec with GuiceOneAppPerSuite with I18nHelper with MockitoSugar with BeforeAndAfter {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockUserDetailsConnector: UserDetailsConnector = mock[UserDetailsConnector]
  val mockSessionService: SessionService = mock[SessionService]
  val mockConfig: Configuration = mock[Configuration]
  val mockEnvironment: Environment = mock[Environment]
  val mockRasConnector: ResidencyStatusAPIConnector = mock[ResidencyStatusAPIConnector]
  val mockAuditService: AuditService = mock[AuditService]

  def configureRasResidencyCheckerController(version: ApiVersion) = new RasResidencyCheckerController {
    override val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val sessionService: SessionService = mockSessionService
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment
    override val residencyStatusAPIConnector: ResidencyStatusAPIConnector = mockRasConnector
    override val auditService: AuditService = mockAuditService
    override val apiVersion: ApiVersion = version
  }

  "RasResidencyCheckerController extractResidencyStatus" when {
    "version 1.0 of the API is used" should {
      "extract the result into the correct messages" in {
        val testRasResidencyCheckerController = configureRasResidencyCheckerController(ApiV1_0)

        testRasResidencyCheckerController.extractResidencyStatus(SCOTTISH) shouldBe Messages("scottish.taxpayer")
        testRasResidencyCheckerController.extractResidencyStatus(WELSH) shouldBe ""
        testRasResidencyCheckerController.extractResidencyStatus(OTHER_UK) shouldBe Messages("non.scottish.taxpayer")
        testRasResidencyCheckerController.extractResidencyStatus("") shouldBe ""
      }
    }
    "version 2.0 of the API is used" should {
      "extract the result into the correct messages" in {
        val testRasResidencyCheckerController = configureRasResidencyCheckerController(ApiV2_0)

        testRasResidencyCheckerController.extractResidencyStatus(SCOTTISH) shouldBe Messages("scottish.taxpayer")
        testRasResidencyCheckerController.extractResidencyStatus(WELSH) shouldBe Messages("welsh.taxpayer")
        testRasResidencyCheckerController.extractResidencyStatus(OTHER_UK) shouldBe Messages("english.or.ni.taxpayer")
        testRasResidencyCheckerController.extractResidencyStatus("") shouldBe ""
      }
    }
  }

}
