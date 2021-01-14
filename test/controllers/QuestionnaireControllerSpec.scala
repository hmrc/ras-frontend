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

import play.api.http.Status
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

class QuestionnaireControllerSpec extends UnitSpec with RasTestHelper {

  override val fakeRequest = FakeRequest("GET", "/")
  val fakePostRequest = FakeRequest("POST", "/signed-out")

  val TestController = new QuestionnaireController(mockAuditConnector, mockMCC, mockAppConfig)

  "Calling the QuestionnaireController.showQuestionnaire" should {
    "respond with OK" in {
      val result = TestController.showQuestionnaire(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "Calling the QuestionnaireController.submitQuestionnaire" should {
    "respond with OK" in {
      val result = TestController.submitQuestionnaire(fakePostRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "Calling the QuestionnaireController.feedbackThankyou" should {
    "respond with OK" in {
      val result = TestController.feedbackThankyou(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }

  "Questionnaire form submission" should {

    "return bad request when form error present" in {
      val postData = Json.obj(
        "easyToUse" -> 1,
        "satisfactionLevel" -> "incorrect",
        "whyGiveThisRating" -> "some feedback")
      val result = TestController.submitQuestionnaire.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe BAD_REQUEST
    }
  }
}
