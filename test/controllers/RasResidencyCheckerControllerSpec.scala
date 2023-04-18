/*
 * Copyright 2023 HM Revenue & Customs
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
import models._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec
import services.SessionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import utils.RasTestHelper

class RasResidencyCheckerControllerSpec extends AnyWordSpec with RasTestHelper {

  def configureRasResidencyCheckerController(version: ApiVersion): RasResidencyCheckerController = new RasResidencyCheckerController {
    override val authConnector: AuthConnector = mockAuthConnector
		override val connector: DefaultAuditConnector = mockAuditConnector
    override val sessionService: SessionService = mockSessionService
    override val residencyStatusAPIConnector: ResidencyStatusAPIConnector = mockResidencyStatusAPIConnector
    override val apiVersion: ApiVersion = version
		override val appConfig: ApplicationConfig = mockAppConfig
  }

  "RasResidencyCheckerController extractResidencyStatus" when {
    "version 1.0 of the API is used" must {
      "extract the result into the correct messages" in {
        val testRasResidencyCheckerController = configureRasResidencyCheckerController(ApiV1_0)

        testRasResidencyCheckerController.extractResidencyStatus(SCOTTISH) shouldBe "Scotland"
        testRasResidencyCheckerController.extractResidencyStatus(WELSH) shouldBe ""
        testRasResidencyCheckerController.extractResidencyStatus(OTHER_UK) shouldBe "England, Northern Ireland or Wales"
        testRasResidencyCheckerController.extractResidencyStatus("") shouldBe ""
      }
    }
    "version 2.0 of the API is used" must {
      "extract the result into the correct messages" in {
        val testRasResidencyCheckerController = configureRasResidencyCheckerController(ApiV2_0)

        testRasResidencyCheckerController.extractResidencyStatus(SCOTTISH) shouldBe "Scotland"
        testRasResidencyCheckerController.extractResidencyStatus(WELSH) shouldBe "Wales"
        testRasResidencyCheckerController.extractResidencyStatus(OTHER_UK) shouldBe "England, Northern Ireland or Wales"
        testRasResidencyCheckerController.extractResidencyStatus("") shouldBe ""
      }
    }
  }

}
