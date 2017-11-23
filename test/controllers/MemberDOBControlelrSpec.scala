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

package controllers

import connectors.{CustomerMatchingAPIConnector, ResidencyStatusAPIConnector, UserDetailsConnector}
import helpers.helpers.I18nHelper
import metrics.Metrics
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{when, _}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import play.api.{Configuration, Environment}
import services.SessionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class MemberDOBControllerSpec extends UnitSpec with WithFakeApplication with I18nHelper with MockitoSugar{

  implicit val headerCarrier = HeaderCarrier()

  val fakeRequest = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockUserDetailsConnector = mock[UserDetailsConnector]
  val mockSessionService = mock[SessionService]
  val mockConfig = mock[Configuration]
  val mockEnvironment = mock[Environment]
  val mockRasConnector = mock[ResidencyStatusAPIConnector]
  val mockMatchingConnector = mock[CustomerMatchingAPIConnector]
  val SCOTTISH = "scotResident"
  val NON_SCOTTISH = "otherUKResident"
  val uuid = "b5a4c95d-93ff-4054-b416-79c8a7e6f712"
  val memberName = MemberName("Jackie","Chan")
  val memberNino = MemberNino("AB123456C")
  val dob = RasDate(Some("12"),Some("12"),Some("2012"))
  val memberDob = MemberDateOfBirth(dob)
  val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),None)
  val postData = Json.obj("dateOfBirth" -> dob)

  object TestMemberDobController extends MemberDOBController{
    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val sessionService = mockSessionService
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment
    override val residencyStatusAPIConnector: ResidencyStatusAPIConnector = mockRasConnector
    override val customerMatchingAPIConnector: CustomerMatchingAPIConnector = mockMatchingConnector

    when(mockSessionService.cacheDob(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
    when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))

    when(customerMatchingAPIConnector.findMemberDetails(any())(any())).thenReturn(Future.successful(Some(uuid)))
    when(residencyStatusAPIConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus(SCOTTISH, NON_SCOTTISH)))

  }

  val successfulRetrieval: Future[~[Option[String], Option[String]]] = Future.successful(new ~(Some("1234"), Some("/")))


  "MemberDobController get" should {

    when(mockAuthConnector.authorise[~[Option[String], Option[String]]](any(), any())(any(),any())).
      thenReturn(successfulRetrieval)

    when(mockUserDetailsConnector.getUserDetails(any())(any())).
      thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))


    "return ok" when {
      "called" in {
        val result = TestMemberDobController.get(fakeRequest)
        status(result) shouldBe OK
      }
    }

    "contain correct page elements and content" when {
      "rendered" in {
        val result = TestMemberDobController.get(fakeRequest)
        doc(result).title shouldBe Messages("member.dob.page.title")
        doc(result).getElementById("header").text shouldBe Messages("member.dob.page.header","Jackie Chan")
        doc(result).getElementById("dob_hint").text shouldBe Messages("dob.hint")
        doc(result).getElementById("continue").text shouldBe Messages("continue")
        doc(result).getElementById("dob-day_label").text shouldBe Messages("Day")
        doc(result).getElementById("dob-month_label").text shouldBe Messages("Month")
        doc(result).getElementById("dob-year_label").text shouldBe Messages("Year")
      }
    }

    "fill in form" when {
      "details returned from session cache" in {
        val result = TestMemberDobController.get(fakeRequest)
        doc(result).getElementById("dob-year").value.toString should include(memberDob.dateOfBirth.year.getOrElse("0"))
        doc(result).getElementById("dob-month").value.toString should include(memberDob.dateOfBirth.month.getOrElse("0"))
        doc(result).getElementById("dob-day").value.toString should include(memberDob.dateOfBirth.day.getOrElse("0"))
      }
    }

    "present empty form" when {
      "no details returned from session cache" in {
        when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val result = TestMemberDobController.get(fakeRequest)
        assert(doc(result).getElementById("dob-year").attr("value").isEmpty)
        assert(doc(result).getElementById("dob-month").attr("value").isEmpty)
        assert(doc(result).getElementById("dob-day").attr("value").isEmpty)
      }
    }
  }

  "Member dob controller form submission" should {

    "respond to POST /relief-at-source/member-details" in {
      val result = route(fakeApplication, FakeRequest(POST, "/relief-at-source/member-details"))
      status(result.get) should not equal (NOT_FOUND)
    }

    "return bad request when form error present" in {
      val postData = Json.obj("dateOfBirth" -> RasDate(Some("0"),Some("1"),Some("1111")))
      val result = TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
    }


    "redirect" in {
      val postData = Json.obj("dateOfBirth" -> RasDate(Some("1"), Some("1"), Some("1999")))
      val result = TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(SEE_OTHER)
    }

    "redirect if current year residency status is empty" in {
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus("", NON_SCOTTISH)))
      val result = TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(SEE_OTHER)
      redirectLocation(result).get should include("global-error")
    }

    "save details to cache" in {
      val result = TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      when(mockSessionService.cacheDob(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      verify(mockSessionService, atLeastOnce()).cacheDob(Matchers.any())(Matchers.any(), Matchers.any())
    }

    "redirect if unknown current year residency status is returned" in {
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus("blah", NON_SCOTTISH)))
      val result = TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe 303
      redirectLocation(result).get should include("global-error")
    }

    "redirect to technical error page if customer matching fails to return a response" in {
      when(mockMatchingConnector.findMemberDetails(any())(any())).thenReturn(Future.failed(new Exception()))
      val result = await(TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData))))
      status(result) shouldBe 303
      redirectLocation(result).get should include("global-error")
    }

    "redirect to technical error page if ras fails to return a response" in {
      when(mockMatchingConnector.findMemberDetails(any())(any())).thenReturn(Future.successful(Some(uuid)))
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.failed(new Exception()))
      val result = await(TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData))))
      status(result) shouldBe 303
      redirectLocation(result).get should include("global-error")
    }

    "redirect to technical error page if no uuid" in {
      when(mockMatchingConnector.findMemberDetails(any())(any())).thenReturn(Future.successful(None))
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.failed(new Exception()))
      val result = TestMemberDobController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe 303
      redirectLocation(result).get should include("global-error")
    }

  }

  "return to member nino page when back link is clicked" in {
    when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
    val result = TestMemberDobController.back.apply(FakeRequest())
    status(result) shouldBe SEE_OTHER
    redirectLocation(result).get should include("/member-nino")
  }

  "redirect to global error page navigating back with no session" in {
    when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestMemberDobController.back.apply(FakeRequest())
    status(result) shouldBe SEE_OTHER
    redirectLocation(result).get should include("global-error")
  }

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

}
