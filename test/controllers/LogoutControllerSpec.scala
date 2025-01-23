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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, include}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments, SessionRecordNotFound}
import utils.RasTestHelper

import scala.concurrent.Future

class LogoutControllerSpec extends AnyWordSpec with RasTestHelper {

  val logoutController = new LogoutController(mockAuthConnector, mockMCC, mockAppConfig)

  "LogoutController" must {

    "redirect to the feedback page when user is logged in" in {
      val enrolment = new Enrolment(
        key = "HMRC-PSA-ORG",
        identifiers = List(EnrolmentIdentifier("PSAID", "Z123456")),
        state = "Activated"
      )

      val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))

      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

      val result: Future[Result] = logoutController.logout(FakeRequest())

      redirectLocation(result) should include("/feedback/ras")
      status(result) shouldBe Status.SEE_OTHER
    }

    "redirect to sign in page when user logged out" in {
      when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
        .thenReturn(Future.failed(SessionRecordNotFound("no session found")))

      val result: Future[Result] = logoutController.logout(FakeRequest())

      redirectLocation(result) should include("gg/sign-in?continue_url=%2Frelief-at-source%2F&origin=ras-frontend")
      status(result) shouldBe Status.SEE_OTHER
    }

  }
}
