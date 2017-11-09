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

import connectors.{FileUploadConnector, UserDetailsConnector}
import helpers.helpers.I18nHelper
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.any
import org.mockito.{Matchers, Mock}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import play.api.{Configuration, Environment}
import services.{UploadService, SessionService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class FileUploadControllerSpec extends UnitSpec with WithFakeApplication with I18nHelper with MockitoSugar{

  implicit val headerCarrier = HeaderCarrier()
  val fakeRequest = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockUserDetailsConnector = mock[UserDetailsConnector]
  val mockSessionService = mock[SessionService]
  val mockConfig = mock[Configuration]
  val mockEnvironment = mock[Environment]
  val mockFileUploadService = mock[UploadService]
  val successfulRetrieval: Future[~[Option[String], Option[String]]] = Future.successful(new ~(Some("1234"), Some("/")))
  val memberName = MemberName("Jackie","Chan")
  val memberNino = MemberNino("AB123456C")
  val memberDob = MemberDateOfBirth(RasDate(Some("12"),Some("12"),Some("2012")))
  val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""))

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  object TestFileUploadController extends FileUploadController {
    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val sessionService = mockSessionService
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment
    override val fileUploadService = mockFileUploadService

    when(mockAuthConnector.authorise[~[Option[String], Option[String]]](any(), any())(any(),any())).
      thenReturn(successfulRetrieval)

    when(mockUserDetailsConnector.getUserDetails(any())(any())).
      thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))

    when(mockFileUploadService.uploadFile).thenReturn(Future.successful(true))

  }

  "FileUploadController" should {

    "return 200" when {
      "called" in {
        val result = TestFileUploadController.get().apply(fakeRequest)
        status(result) shouldBe OK
      }
    }

    "contain 'upload file' title and header" in {
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).title() shouldBe Messages("file.upload.page.title")
      doc(result).getElementById("header").text shouldBe Messages("file.upload.page.header")
    }

    "contain sub-header" in {
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementById("sub-header").html shouldBe Messages("file.upload.page.sub-header",Messages("templates.link"))
    }

    "contain 'choose file' button" in {
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementById("choose-file") shouldNot be(null)
    }

    "contain an upload button" in {
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementById("upload-button").attr("value") shouldBe Messages("upload")
    }

    "contain a back link" in {
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementsByClass("link-back").text shouldBe Messages("back")
    }

    "redirect" when {
      "file is uploaded successfully" in {
        val result = await(TestFileUploadController.post.apply(fakeRequest))
        status(result) shouldBe 303
        redirectLocation(result).get should include("/bulk/upload-successful")
      }
    }

    "return to dashboard page when back link is clicked" in {
      val result = TestFileUploadController.back.apply(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("/dashboard")
    }

  }


}
