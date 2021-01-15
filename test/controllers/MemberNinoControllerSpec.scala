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

import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{atLeastOnce, verify, when}
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.test.UnitSpec
import utils.{RandomNino, RasTestHelper}

import scala.concurrent.Future

class MemberNinoControllerSpec extends UnitSpec with RasTestHelper {

  val memberName: MemberName = MemberName("Jackie", "Chan")
  val memberNino: MemberNino = MemberNino("AB123456C")
  val memberDob: MemberDateOfBirth = MemberDateOfBirth(RasDate(Some("12"), Some("12"), Some("2012")))
  val rasSession: RasSession = RasSession(memberName, memberNino, memberDob, None, None)
  val postData: JsObject = Json.obj("nino" -> RandomNino.generate)

  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")
  private val enrolment = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
  private val enrolments = Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)

  val TestMemberNinoController: MemberNinoController = new MemberNinoController(mockAuthConnector, mockResidencyStatusAPIConnector, mockAuditConnector, mockShortLivedCache, mockSessionService, mockMCC, mockAppConfig) {
    override lazy val apiVersion: ApiVersion = ApiV1_0

    when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
    when(mockSessionService.cacheNino(any())(any())).thenReturn(Future.successful(Some(rasSession)))
  }

  "MemberNinoController get" should {

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

    when(mockUserDetailsConnector.getUserDetails(any())(any(), any())).
      thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))

    "return ok" when {
      "called" in {
        val result = TestMemberNinoController.get()(fakeRequest)
        status(result) shouldBe OK
      }

      "called without an existing ras session" in {
        when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(None))
        val result = TestMemberNinoController.get()(fakeRequest)
        status(result) shouldBe OK
      }
    }
  }

  "MemberNinoController post" should {

    "return bad request with session name when form error is present and session contains a name" in {
      when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      val postData = Json.obj(
        "nino" -> RandomNino.generate.substring(3))
      val result = TestMemberNinoController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
    }

    "return bad request with member as name when form error is present and session contains no name" in {
      when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(None))
      val postData = Json.obj(
        "nino" -> RandomNino.generate.substring(3))
      val result = TestMemberNinoController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
    }

    "return bad request when form error present with special characters" in {
      val postData = Json.obj(
        "nino" -> "AB123%56C")
      val result = TestMemberNinoController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
    }

    "redirect to dob page when nino cached and edit mode is false" in {
      when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestMemberNinoController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("member-date-of-birth")
    }

    "redirect to match found page when edit mode is true and matching successful" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any())).thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))
      when(mockSessionService.cacheResidencyStatusResult(any())(any())).thenReturn(Future.successful(Some(rasSession)))

      val result = TestMemberNinoController.post(true).apply(fakeRequest.withJsonBody(Json.toJson(postData)))

      status(result) should equal(SEE_OTHER)
      redirectLocation(result).get should include("/member-residency-status")

      verify(mockSessionService, atLeastOnce()).cacheNino(any())(any())
    }

    "redirect to no match found page when edit mode is true and matching failed" in {
      when(mockResidencyStatusAPIConnector.getResidencyStatus(any())(any(), any())).thenReturn(Future.failed(UpstreamErrorResponse("Member not found", 403, 403)))

      val result = TestMemberNinoController.post(true).apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(SEE_OTHER)
      redirectLocation(result).get should include("/no-residency-status-displayed")

      verify(mockSessionService, atLeastOnce()).cacheNino(any())(any())
    }

    "redirect to technical error page if nino is not cached" in {
      when(mockSessionService.cacheNino(any())(any())).thenReturn(Future.successful(None))
      val result = TestMemberNinoController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("global-error")
    }

  }

  "return to member name page when back link is clicked and edit mode is false" in {
    when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
    val result = TestMemberNinoController.back().apply(FakeRequest())
    status(result) shouldBe SEE_OTHER
    redirectLocation(result).get should include("/member-name")
  }

  "return to match not found page when back link is clicked and edit mode is true" in {
    when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(Some(rasSession)))
    val result = TestMemberNinoController.back(true).apply(FakeRequest())
    status(result) shouldBe SEE_OTHER
    redirectLocation(result).get should include("/no-residency-status-displayed")
  }

  "redirect to global error page navigating back with no session" in {
    when(mockSessionService.fetchRasSession()(any())).thenReturn(Future.successful(None))
    val result = TestMemberNinoController.back().apply(FakeRequest())
    status(result) shouldBe SEE_OTHER
    redirectLocation(result).get should include("global-error")
  }
}
