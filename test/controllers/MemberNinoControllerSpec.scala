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

import connectors.UserDetailsConnector
import helpers.RandomNino
import helpers.helpers.I18nHelper
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
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
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future


class MemberNinoControllerSpec extends UnitSpec with WithFakeApplication with I18nHelper with MockitoSugar{

  val fakeRequest = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockUserDetailsConnector = mock[UserDetailsConnector]
  val mockSessionService = mock[SessionService]
  val mockConfig = mock[Configuration]
  val mockEnvironment = mock[Environment]

  val memberName: MemberName = MemberName("Jackie","Chan")
  val memberNino = MemberNino("AB123456C")
  val memberDob = MemberDateOfBirth(RasDate(Some("12"),Some("12"),Some("2012")))
  val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),None)
  val postData = Json.obj("nino" -> RandomNino.generate)

  object TestMemberNinoController extends MemberNinoController{
    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val sessionService = mockSessionService
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment

    when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
    when(mockSessionService.cacheNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
  }

  val successfulRetrieval: Future[~[Option[String], Option[String]]] = Future.successful(new ~(Some("1234"), Some("/")))

  "MemberNinoController get" should {

    when(mockAuthConnector.authorise[~[Option[String], Option[String]]](any(), any())(any(),any())).
      thenReturn(successfulRetrieval)

    when(mockUserDetailsConnector.getUserDetails(any())(any())).
      thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))

    "return ok" when {
      "called" in {
        val result = TestMemberNinoController.get(fakeRequest)
        status(result) shouldBe OK
      }
    }

    "contain correct page elements and content" when {
      "rendered" in {
        val result = TestMemberNinoController.get(fakeRequest)
        doc(result).title shouldBe Messages("member.nino.page.title")
        doc(result).getElementById("header").text shouldBe Messages("member.nino.page.header","Jackie Chan")
        doc(result).getElementById("nino_hint").text shouldBe Messages("nino.hint")
        assert(doc(result).getElementById("nino").attr("input") != null)
        doc(result).getElementById("continue").text shouldBe Messages("continue")
      }

      "rendered but no cached data exists" in {
        when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val result = TestMemberNinoController.get(fakeRequest)
        doc(result).title shouldBe Messages("member.nino.page.title")
        doc(result).getElementById("header").text shouldBe Messages("member.nino.page.header",Messages("member"))
      }
    }

  }

  "MemberNinoController post" should {

    "respond to POST /relief-at-source/member-nino" in {
      val result = route(fakeApplication, FakeRequest(POST, "/relief-at-source/member-nino"))
      status(result.get) should not equal (NOT_FOUND)
    }

    "return bad request when form error present" in {
      val postData = Json.obj(
        "nino" -> RandomNino.generate.substring(3))
      val result = TestMemberNinoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
    }

    "return bad request when form error present with special characters" in {
      val postData = Json.obj(
        "nino" -> "AB123%56C")
      val result = TestMemberNinoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
    }

    "redirect to dob page when nino cached" in {
      val result = TestMemberNinoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("member-date-of-birth")
    }

    "redirect to technical error page if nino is not cached" in {
      when(mockSessionService.cacheNino(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      val result = TestMemberNinoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("global-error")
    }

  }

  "return to member name page when back link is clicked" in {
    when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
    val result = TestMemberNinoController.back.apply(FakeRequest())
    status(result) shouldBe SEE_OTHER
    redirectLocation(result).get should include("/member-name")
  }

  "redirect to global error page navigating back with no session" in {
    when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestMemberNinoController.back.apply(FakeRequest())
    status(result) shouldBe SEE_OTHER
    redirectLocation(result).get should include("global-error")
  }

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

}
