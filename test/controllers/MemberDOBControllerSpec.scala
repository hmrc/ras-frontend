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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => Meq}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import services.{AuditService, SessionService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.{Generator, PsaId}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import scala.util.Random

class MemberDOBControllerSpec extends UnitSpec with WithFakeApplication with I18nHelper with MockitoSugar with BeforeAndAfter {

  implicit val headerCarrier = HeaderCarrier()

  def randomPsaId: Gen[PsaId] =
    for {
      prefix <- Gen.alphaUpperChar
      number <- Gen.chooseNum(0, 9999999)
    } yield {
      PsaId(f"$prefix$number%07d")
    }

  private val fakeRequest = FakeRequest()
  private val mockAuthConnector = mock[AuthConnector]
  private val mockUserDetailsConnector = mock[UserDetailsConnector]
  private val mockSessionService = mock[SessionService]
  private val mockConfig = mock[Configuration]
  private val mockEnvironment = mock[Environment]
  private val mockRasConnector = mock[ResidencyStatusAPIConnector]
  private val mockAuditService = mock[AuditService]
  private val memberName = MemberName("Jackie", "Chan")
  private val memberNino: String = new Generator(new Random()).nextNino.nino
  private val dob = RasDate(Some("12"), Some("12"), Some("2012"))
  private val memberDob = MemberDateOfBirth(dob)
  private val rasSession = RasSession(memberName, MemberNino(memberNino), memberDob, None, None)
  private val postData = Json.obj("dateOfBirth" -> dob)
  private val psaId: String = randomPsaId.sample.get.id
  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", psaId)
  private val enrolment = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
  private val enrolments = new Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)

  object TestMemberDobController extends MemberDOBController {
    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val sessionService = mockSessionService
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment
    override val residencyStatusAPIConnector: ResidencyStatusAPIConnector = mockRasConnector
    override val auditService: AuditService = mockAuditService
    override val apiVersion: ApiVersion = ApiV1_0

    when(mockSessionService.fetchRasSession()(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
  }

  before {
    reset(mockAuditService)
  }

  "MemberDobController get" should {

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)

    when(mockUserDetailsConnector.getUserDetails(any())(any())).
      thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))

    when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))

    "return ok" when {
      "called" in {
        val result = TestMemberDobController.get()(fakeRequest)
        status(result) shouldBe OK
      }
    }

    "contain correct page elements and content" when {
      "rendered" in {
        val result = TestMemberDobController.get()(fakeRequest)
        doc(result).title shouldBe Messages("member.dob.page.title")
        doc(result).getElementById("header").text shouldBe Messages("member.dob.page.header", "Jackie Chan")
        doc(result).getElementById("dob_hint").text shouldBe Messages("dob.hint")
        doc(result).getElementById("continue").text shouldBe Messages("continue")
        doc(result).getElementById("dateOfBirth-day_label").text shouldBe Messages("Day")
        doc(result).getElementById("dateOfBirth-month_label").text shouldBe Messages("Month")
        doc(result).getElementById("dateOfBirth-year_label").text shouldBe Messages("Year")
      }

      "contain the correct ga data when edit mode is false" in {
        val result = TestMemberDobController.get()(fakeRequest)
        doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:What is their DOB?:Continue"
        doc(result).getElementsByClass("link-back").attr("data-journey-click") shouldBe "navigation - link:What is their DOB?:Back"
      }

      "contain the correct ga data when edit mode is true" in {
        val result = TestMemberDobController.get(true)(fakeRequest)
        doc(result).getElementById("continue").attr("data-journey-click") shouldBe "button - click:What is their DOB?:Continue and submit"
      }
    }

    "fill in form" when {
      "details returned from session cache" in {
        val result = TestMemberDobController.get()(fakeRequest)
        doc(result).getElementById("dateOfBirth-year").value.toString should include(memberDob.dateOfBirth.year.getOrElse("0"))
        doc(result).getElementById("dateOfBirth-month").value.toString should include(memberDob.dateOfBirth.month.getOrElse("0"))
        doc(result).getElementById("dateOfBirth-day").value.toString should include(memberDob.dateOfBirth.day.getOrElse("0"))
      }
    }

    "present empty form" when {
      "no details returned from session cache" in {
        when(mockSessionService.fetchRasSession()(Matchers.any())).thenReturn(Future.successful(None))
        val result = TestMemberDobController.get()(fakeRequest)
        assert(doc(result).getElementById("dateOfBirth-year").attr("value").isEmpty)
        assert(doc(result).getElementById("dateOfBirth-month").attr("value").isEmpty)
        assert(doc(result).getElementById("dateOfBirth-day").attr("value").isEmpty)
      }
    }
  }

  "Member dob controller form submission" should {

    "respond to POST /relief-at-source/member-details" in {
      val result = route(fakeApplication, FakeRequest(POST, "/relief-at-source/member-details"))
      status(result.get) should not equal (NOT_FOUND)
    }

    "return bad request with session name when form error present and session has a name" in {
      when(mockSessionService.fetchRasSession()(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val postData = Json.obj("dateOfBirth" -> RasDate(Some("0"), Some("1"), Some("1111")))
      val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
      doc(result).getElementById("header").text shouldBe Messages("member.dob.page.header", "Jackie Chan")
    }

    "return bad request with member as name when form error present and session has no name" in {
      when(mockSessionService.fetchRasSession()(Matchers.any())).thenReturn(Future.successful(None))
      val postData = Json.obj("dateOfBirth" -> RasDate(Some("0"), Some("1"), Some("1111")))
      val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
      doc(result).getElementById("header").text shouldBe Messages("member.dob.page.header", Messages("member"))
    }

    "redirect" in {
      when(mockSessionService.cacheDob(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))

      val postData = Json.obj("dateOfBirth" -> RasDate(Some("1"), Some("1"), Some("1999")))
      val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(SEE_OTHER)
    }

    "redirect if current year residency status is empty" in {
      when(mockSessionService.cacheDob(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus("", Some(OTHER_UK))))
      val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(SEE_OTHER)
      redirectLocation(result).get should include("global-error")
    }

    "save details to cache" in {
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))

      val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      when(mockSessionService.cacheDob(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
      verify(mockSessionService, atLeastOnce()).cacheDob(Matchers.any())(Matchers.any())
    }

    "redirect if unknown current year residency status is returned" in {
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus("blah", Some(OTHER_UK))))
      val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe 303
      redirectLocation(result).get should include("global-error")
    }

    "redirect to technical error page if customer matching fails to return a response" in {
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.failed(new Exception()))
      val result = await(TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData))))
      status(result) shouldBe 303
      redirectLocation(result).get should include("global-error")
    }

    "redirect to technical error page if ras fails to return a response" in {
      when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.failed(new Exception()))
      val result = await(TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData))))
      status(result) shouldBe 303
      redirectLocation(result).get should include("global-error")
    }

    "return to member nino page when back link is clicked and edit is false" in {
      when(mockSessionService.fetchRasSession()(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestMemberDobController.back().apply(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("/member-national-insurance-number")
    }

    "return to not found page when back link is clicked and edit is true" in {
      when(mockSessionService.fetchRasSession()(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestMemberDobController.back(true).apply(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("/no-residency-status-displayed")
    }

    "redirect to global error page navigating back with no session" in {
      when(mockSessionService.fetchRasSession()(Matchers.any())).thenReturn(Future.successful(None))
      val result = TestMemberDobController.back().apply(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("global-error")
    }

    "redirect to match found page" when {
      "a request is made which returns CY and NY results (i.e. before 6th April)" in {

        when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus(SCOTTISH, Some(OTHER_UK))))
        when(mockSessionService.cacheDob(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockSessionService.cacheResidencyStatusResult(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))

        val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))

        status(result) should equal(SEE_OTHER)
        redirectLocation(result).get should include("/member-residency-status")

        verify(mockSessionService, atLeastOnce()).cacheDob(Matchers.any())(Matchers.any())

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq("/"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "scotResident",
            "NextCYStatus" -> "otherUKResident",
            "nino" -> memberNino,
            "userIdentifier" -> psaId,
            "requestSource" -> "FE_SINGLE"))
        )(any())
      }

      "a request is made which returns only a CY result (i.e. after 6th April)" in {

        when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.successful(ResidencyStatus(SCOTTISH, None)))
        when(mockSessionService.cacheDob(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockSessionService.cacheResidencyStatusResult(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(rasSession)))

        val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))

        status(result) should equal(SEE_OTHER)
        redirectLocation(result).get should include("/member-residency-status")

        verify(mockSessionService, atLeastOnce()).cacheDob(Matchers.any())(Matchers.any())

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq("/"),
          auditData = Meq(Map("successfulLookup" -> "true",
            "CYStatus" -> "scotResident",
            "nino" -> memberNino,
            "userIdentifier" -> psaId,
            "requestSource" -> "FE_SINGLE"))
        )(any())
      }
    }

    "redirect to no match found page" when {
      "matching failed is returned from the connector" in {

        when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.failed(Upstream4xxResponse("Member not found", 403, 403)))

        val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
        status(result) should equal(SEE_OTHER)
        redirectLocation(result).get should include("/no-residency-status-displayed")

        verify(mockSessionService, atLeastOnce()).cacheDob(Matchers.any())(Matchers.any())

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq("/"),
          auditData = Meq(Map("successfulLookup" -> "false",
            "reason" -> "MATCHING_FAILED",
            "nino" -> memberNino,
            "userIdentifier" -> psaId,
            "requestSource" -> "FE_SINGLE"))
        )(any())
      }
    }

    "redirect to global error page" when {
      "an exception is thrown by submitResidencyStatus" in {
        when(mockRasConnector.getResidencyStatus(any())(any())).thenReturn(Future.failed(Upstream4xxResponse("Unknown Error", 500, 500)))

        val result = TestMemberDobController.post().apply(fakeRequest.withJsonBody(Json.toJson(postData)))
        status(result) should equal(SEE_OTHER)
        redirectLocation(result).get should include("/global-error")

        verify(mockSessionService, atLeastOnce()).cacheDob(Matchers.any())(Matchers.any())

        verify(mockAuditService).audit(
          auditType = Meq("ReliefAtSourceResidency"),
          path = Meq("/"),
          auditData = Meq(Map("successfulLookup" -> "false",
            "reason" -> "INTERNAL_SERVER_ERROR",
            "nino" -> memberNino,
            "userIdentifier" -> psaId,
            "requestSource" -> "FE_SINGLE"))
        )(any())
      }
    }
  }

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

}
