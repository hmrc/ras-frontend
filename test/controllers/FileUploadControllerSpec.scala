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
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import play.api.{Configuration, Environment}
import services.SessionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class FileUploadControllerSpec extends UnitSpec with WithFakeApplication with I18nHelper with MockitoSugar{

  implicit val headerCarrier = HeaderCarrier()

  val fakeRequest = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockFileUploadConnector = mock[FileUploadConnector]
  val mockUserDetailsConnector = mock[UserDetailsConnector]
  val mockSessionService = mock[SessionService]
  val mockConfig = mock[Configuration]
  val mockEnvironment = mock[Environment]
  val successfulRetrieval: Future[~[Option[String], Option[String]]] = Future.successful(new ~(Some("1234"), Some("/")))
  val memberName = MemberName("Jackie","Chan")
  val memberNino = MemberNino("AB123456C")
  val memberDob = MemberDateOfBirth(RasDate(Some("12"),Some("12"),Some("2012")))
  val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""))
  val connectorResponse = HttpResponse(201,None,Map("Location" -> List("localhost:8898/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653")),None)


  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  object TestFileUploadController extends FileUploadController {
    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val sessionService = mockSessionService
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment
    override val fileUploadConnector = mockFileUploadConnector

    when(mockAuthConnector.authorise[~[Option[String], Option[String]]](any(), any())(any(),any())).thenReturn(successfulRetrieval)
    when(mockUserDetailsConnector.getUserDetails(any())(any())).thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))
  }

  "FileUploadController" should {

    "use existing envelope id to construct a url for the upload form" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get.apply(fakeRequest))
      status(result) shouldBe OK
      doc(result).getElementById("upload-form").attr("action").contains("existingEnvelopeId123")
    }

    "use new envelope id to construct a url for the upload form" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, None)
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      when(mockFileUploadConnector.createEnvelope()(any())).thenReturn(Future.successful(connectorResponse))
      val result = await(TestFileUploadController.get.apply(fakeRequest))
      status(result) shouldBe OK
      doc(result).getElementById("upload-form").attr("action").contains("0b215e97-11d4-4006-91db-c067e74fc653")
    }

    "redirect to global error page" when {

      "an upload url has not been obtained because of empty location header" in {
        val connectorResponse = HttpResponse(201,None,Map("Location" -> List("localhost:8898/file-upload/envelopes/")),None)
        when(mockFileUploadConnector.createEnvelope()(any())).thenReturn(Future.successful(connectorResponse))
        val result = TestFileUploadController.get().apply(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }

      "an upload url has not been obtained because of bad request" in {
        val connectorResponse = HttpResponse(400,None,Map(),None)
        when(mockFileUploadConnector.createEnvelope()(any())).thenReturn(Future.successful(connectorResponse))
        val result = TestFileUploadController.get().apply(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }
      
      "the upload error endpoint in called by the file upload but caching fails" in {
        when(mockSessionService.cacheUploadResponse(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        val uploadRequest = FakeRequest(GET, "/relief-at-source/upload-error?errorCode=400&reason={%22error%22:{%22msg%22:%22Envelope%20does%20not%20allow%20zero%20length%20files,%20and%20submitted%20file%20has%20length%200%22}}" )
        val result = await(TestFileUploadController.uploadError().apply(uploadRequest))
        redirectLocation(result).get should include("/global-error")
      }

      "there has been a problem calling the envelope creator" in {
        when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(mockFileUploadConnector.createEnvelope()).thenReturn(Future.failed(new RuntimeException))
        val result = TestFileUploadController.get().apply(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }

      "there has been a problem calling the envelope creator after retrieving session" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, None)
        when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        when(mockFileUploadConnector.createEnvelope()).thenReturn(Future.failed(new RuntimeException))
        val result = TestFileUploadController.get().apply(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }
    }

    "render the file upload page containing a back link" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementsByClass("link-back").text shouldBe Messages("back")
    }

    "redirect to dashboard page when back link is clicked" in {
      val result = TestFileUploadController.back.apply(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get should include("/dashboard")
    }

    "display file upload successful page" when {
      "file has been uploaded successfully" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
        when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestFileUploadController.uploadSuccess().apply(fakeRequest))
        status(result) shouldBe OK
      }
    }

    "successful upload page" should {
      "contain the correct content" in {
        val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
        when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        val result = await(TestFileUploadController.uploadSuccess().apply(fakeRequest))
        doc(result).getElementById("page-header").text shouldBe Messages("upload.success.header")
        doc(result).getElementById("page-sub-header").text shouldBe Messages("upload.success.sub-header")
        doc(result).getElementById("continue").text shouldBe Messages("continue")
      }
    }

    "redirect to file upload page" when {
      "the upload error endpoint in called by the file upload" in {
        when(mockSessionService.cacheUploadResponse(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
        val uploadRequest = FakeRequest(GET, "/relief-at-source/upload-error?errorCode=400&reason={%22error%22:{%22msg%22:%22Envelope%20does%20not%20allow%20zero%20length%20files,%20and%20submitted%20file%20has%20length%200%22}}")
        val result = await(TestFileUploadController.uploadError().apply(uploadRequest))
        redirectLocation(result).get should include("bulk/bulk-upload")
      }
    }
  }

  "rendered file upload page" should {

    "contain 'upload file' title and header" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).title() shouldBe Messages("file.upload.page.title")
      doc(result).getElementById("header").text shouldBe Messages("file.upload.page.header")
    }

    "contain sub-header" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementById("sub-header").html shouldBe Messages("file.upload.page.sub-header", Messages("templates.link"))
    }

    "contain 'choose file' button" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementById("choose-file") shouldNot be(null)
    }

    "contain an upload button" in {
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("", "", "", "", "", "", ""), None, Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = TestFileUploadController.get().apply(fakeRequest)
      doc(result).getElementById("upload-button").text shouldBe Messages("upload")
    }

    "contain empty file error if present in session cache" in {
      val uploadResponse = UploadResponse("400",Some(Messages("file.upload.empty.file.reason")))
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),Some(uploadResponse),Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe Messages("file.empty.error")
    }

    "contain upload file error if a bad request has been submitted" in {
      val uploadResponse = UploadResponse("400",None)
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),Some(uploadResponse),Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe Messages("upload.failed.error")
    }

    "contain file too large error if present in session cache" in {
      val uploadResponse = UploadResponse("413",Some(""))
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),Some(uploadResponse),Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe Messages("file.large.error")
    }

    "contain upload failed error if envelope not found in session cache" in {
      val uploadResponse = UploadResponse("404",Some(""))
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),Some(uploadResponse),Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe Messages("upload.failed.error")
    }

    "contain upload failed error if file type is wrong" in {
      val uploadResponse = UploadResponse("415",Some(""))
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),Some(uploadResponse),Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe Messages("upload.failed.error")
    }

    "contain upload failed error if locked" in {
      val uploadResponse = UploadResponse("423",Some(""))
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),Some(uploadResponse),Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe Messages("upload.failed.error")
    }

    "contain file failed to upload if present in session cache" in {
      val uploadResponse = UploadResponse("",Some(""))
      val rasSession = RasSession(memberName, memberNino, memberDob, ResidencyStatusResult("","","","","","",""),Some(uploadResponse),Some(Envelope("existingEnvelopeId123")))
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe Messages("upload.failed.error")
    }

    "not contain any errors if no session cache is available" in {
      when(mockSessionService.fetchRasSession()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
      when(mockFileUploadConnector.createEnvelope()(any())).thenReturn(Future.successful(connectorResponse))
      when(mockSessionService.cacheEnvelope(any())(any(),any())).thenReturn(Future.successful(Some(rasSession)))
      val result = await(TestFileUploadController.get().apply(fakeRequest))
      doc(result).getElementById("upload-error").text shouldBe ""
    }
  }


}
