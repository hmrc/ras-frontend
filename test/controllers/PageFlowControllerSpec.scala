/*
 * Copyright 2021 HM Revenue & Customs
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
import models._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

class PageFlowControllerSpec extends UnitSpec with RasTestHelper {

  object TestPageFlowController extends PageFlowController{
    override val authConnector: AuthConnector = mockAuthConnector
		override val appConfig: ApplicationConfig = mockAppConfig
  }

  val emptySession = RasSession(MemberName("",""),MemberNino(""),
    MemberDateOfBirth(RasDate(None,None,None)),None,None)

  "PageFlowController" should {

    "redirect to choose an option page" when {
      "on member name page and edit is false" in {
        val result = TestPageFlowController.previousPage("MemberNameController")
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/")
      }
    }

    "redirect to match not found page" when {
      "on member name page and edit is true" in {
        val result = TestPageFlowController.previousPage("MemberNameController", edit = true)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/no-residency-status-displayed")
      }
    }

    "redirect to member name page" when {
      "on member nino page and edit is false" in {
        val result = TestPageFlowController.previousPage("MemberNinoController")
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/member-name")
      }
    }

    "redirect to match not found page" when {
      "on member nino page and edit is true" in {
        val result = TestPageFlowController.previousPage("MemberNinoController", edit = true)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/no-residency-status-displayed")
      }
    }

    "redirect to member nino page" when {
      "on member dob page and edit is false" in {
        val result = TestPageFlowController.previousPage("MemberDOBController")
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/member-national-insurance-number")
      }
    }

    "redirect to match not found page" when {
      "on member dob page and edit is true" in {
        val result = TestPageFlowController.previousPage("MemberDOBController", edit = true)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/no-residency-status-displayed")
      }
    }

    "redirect to member dob page" when {
      "on results page" in {
        val result = TestPageFlowController.previousPage("ResultsController")
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/member-date-of-birth")
      }
    }

    "redirect to global error page" when {
      "not found" in {
        val result = TestPageFlowController.previousPage("blahblah")
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }
    }

  }

}
