/*
 * Copyright 2018 HM Revenue & Customs
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

import java.io.ByteArrayInputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import connectors.{ResidencyStatusAPIConnector, UserDetailsConnector}
import forms.WhatDoYouWantToDoForm
import helpers.helpers.I18nHelper
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{OK, contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import play.api.{Configuration, Environment}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future


class WhatDoYouWantToDoControllerSpec extends UnitSpec with MockitoSugar with I18nHelper with WithFakeApplication {

  implicit val headerCarrier = HeaderCarrier()
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val fakeRequest = FakeRequest()
  val mockAuthConnector = mock[AuthConnector]
  val mockUserDetailsConnector = mock[UserDetailsConnector]
  val mockSessionService = mock[SessionService]
  val mockShortLivedCache = mock[ShortLivedCache]
  val mockConfig = mock[Configuration]
  val mockEnvironment = mock[Environment]
  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")
  private val enrolment = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated", ConfidenceLevel.L500)
  private val enrolments = new Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)
  val mockRasConnector = mock[ResidencyStatusAPIConnector]
  val mockUploadTimeStamp = new DateTime().minusDays(10).getMillis
  val mockExpiryTimeStamp = new DateTime().minusDays(7).getMillis
  val mockResultsFileMetadata = ResultsFileMetaData("",None,Some(mockUploadTimeStamp),1,1L)
  val fileSession = FileSession(Some(CallbackData("","someFileId","",None)),Some(mockResultsFileMetadata),"1234",None)
  val userChoice = ""
  val rasSession = RasSession(userChoice ,MemberName("",""),MemberNino(""),MemberDateOfBirth(RasDate(None,None,None)),ResidencyStatusResult("","","","","","",""))

  val row1 = "John,Smith,AB123456C,1990-02-21"
  val inputStream = new ByteArrayInputStream(row1.getBytes)

  object TestWhatDoYouWantToDoController extends WhatDoYouWantToDoController {

    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val resultsFileConnector: ResidencyStatusAPIConnector = mockRasConnector
    override val sessionService = mockSessionService
    override val shortLivedCache = mockShortLivedCache
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
    when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
    when(mockRasConnector.getFile(any())(any())).thenReturn(Future.successful(Some(inputStream)))
    when(mockUserDetailsConnector.getUserDetails(any())(any())).thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))
  }

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  "get" should {

    "respond to GET /relief-at-source/what-do-you-want-to-do" in {
      val result = route(fakeApplication, FakeRequest(GET, "/relief-at-source/what-do-you-want-to-do"))
      status(result.get) should not equal (NOT_FOUND)
    }

    "respond to GET /what-do-you-want-to-do" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestWhatDoYouWantToDoController.get(fakeRequest)
      status(result) shouldBe OK
    }

    "contain the correct title and header" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestWhatDoYouWantToDoController.get(fakeRequest)
      doc(result).title shouldBe Messages("whatDoYouWantToDo.page.title")
      doc(result).getElementById("header").text shouldBe Messages("whatDoYouWantToDo.page.header")
    }

    "contain three options as radio buttons when no file is in progress" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestWhatDoYouWantToDoController.get(fakeRequest)
      doc(result).getElementById("userChoice-single-lookup").attr("value") shouldBe Messages("single.lookup.radio")
      doc(result).getElementById("userChoice-bulk-lookup").attr("value") shouldBe Messages("bulk.lookup.radio")
      doc(result).getElementById("userChoice-result").attr("value") shouldBe Messages("result.radio")
    }

    "redirect to global error page" when {
      "no file session is available" in {
        when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
        when(mockShortLivedCache.fetchFileSession(any())(any())) thenReturn (Future.successful(None))
        val result = TestWhatDoYouWantToDoController.get(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("global-error")
      }

      "no results file is available" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "", None)), None, "1234", None)
        when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
        when(mockShortLivedCache.fetchFileSession(any())(any())) thenReturn (Future.successful(Some(fileSession)))
        val result = TestWhatDoYouWantToDoController.get(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("global-error")
      }

      "no upload timestamp is available" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "", None)), None, "1234", None)
        when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
        when(mockShortLivedCache.fetchFileSession(any())(any())) thenReturn (Future.successful(Some(fileSession)))
        val result = TestWhatDoYouWantToDoController.get(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("global-error")
      }
    }
  }

  "post" should {

    "respond with bad request, must choose option" in {
      val postData = Json.obj("userChoice" -> "")
      val result = TestWhatDoYouWantToDoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      status(result) should equal(BAD_REQUEST)
    }

    "redirect to member name page when single lookup option is selected" in {
      val rasSession = RasSession(WhatDoYouWantToDo.SINGLE ,MemberName("",""),MemberNino(""),MemberDateOfBirth(RasDate(None,None,None)),ResidencyStatusResult("","","","","","",""))
      when(mockSessionService.cacheWhatDoYouWantToDo(any())(any(),any())).thenReturn(Future.successful(Some(rasSession)))
      val postData = Json.obj("userChoice" -> WhatDoYouWantToDo.SINGLE)
      val result = TestWhatDoYouWantToDoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      redirectLocation(result).get should include("/member-name")
    }

    "redirect to file upload page when bulk lookup option is selected" in {
      val rasSession = RasSession(WhatDoYouWantToDo.BULK ,MemberName("",""),MemberNino(""),MemberDateOfBirth(RasDate(None,None,None)),ResidencyStatusResult("","","","","","",""))
      when(mockSessionService.cacheWhatDoYouWantToDo(any())(any(),any())).thenReturn(Future.successful(Some(rasSession)))
      val postData = Json.obj("userChoice" -> WhatDoYouWantToDo.BULK)
      val result = TestWhatDoYouWantToDoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      redirectLocation(result).get should include("/upload-a-file")
    }

    "redirect to file result page when result option is selected" in {
      val rasSession = RasSession(WhatDoYouWantToDo.RESULT ,MemberName("",""),MemberNino(""),MemberDateOfBirth(RasDate(None,None,None)),ResidencyStatusResult("","","","","","",""))
      when(mockSessionService.cacheWhatDoYouWantToDo(any())(any(),any())).thenReturn(Future.successful(Some(rasSession)))
      val postData = Json.obj("userChoice" -> WhatDoYouWantToDo.RESULT)
      val result = TestWhatDoYouWantToDoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
      redirectLocation(result).get should include("/residency-status-added")
    }

    "redirect to global error page" when {
      "no option is selected" in {
        when(mockSessionService.cacheWhatDoYouWantToDo(any())(any(),any())).thenReturn(Future.successful(Some(rasSession)))
        val postData = Json.obj("userChoice" -> WhatDoYouWantToDo.RESULT)
        val result = TestWhatDoYouWantToDoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
        redirectLocation(result).get should include("/global-error")
      }

      "no session has been retrieved" in {
        when(mockSessionService.cacheWhatDoYouWantToDo(any())(any(),any())).thenReturn(Future.successful(None))
        val postData = Json.obj("userChoice" -> WhatDoYouWantToDo.RESULT)
        val result = TestWhatDoYouWantToDoController.post.apply(fakeRequest.withJsonBody(Json.toJson(postData)))
        redirectLocation(result).get should include("/global-error")
      }
    }
  }

  "renderUploadResultsPage" should {

    "respond to GET /relief-at-source/residency-status-added" in {
      val result = route(fakeApplication, FakeRequest(GET, "/relief-at-source/residency-status-added"))
      status(result.get) should not equal (NOT_FOUND)
    }

    "return ok when called" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      status(result) shouldBe OK
    }

    "contain the correct page title" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).title shouldBe Messages("upload.result.page.title")
    }

    "contain a back link pointing to" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("back").attr("href") should include("what-do-you-want-to-do")
    }

    "contain the correct page header" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("page-header").text shouldBe Messages("upload.result.page.header")
    }

    "contain a document header" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("document").text shouldBe Messages("document")
    }

    "contain a document image" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("document-image").attr("src") should include("download-the-file.png")
    }

    "contain a document image that points to get results file" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("document-image-link").attr("href") should include(s"/results/${fileSession.userFile.get.fileId}")
    }

    "contain a result link with the correct file name" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("result-link").text shouldBe Messages("residency.status.result")
    }

    "contain a result link pointing to the results file" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("result-link").attr("href") should include(s"/results/${fileSession.userFile.get.fileId}")
    }

    "contain type and size of the file" in {
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("file-type-size").text shouldBe Messages("file.type.size")
    }

    "contain expiry date message" in {
      val expiryDate = new DateTime(mockExpiryTimeStamp).toString("EEEE d MMMM yyyy")
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("expiry-date-message").text shouldBe Messages("expiry.date.message",expiryDate)
    }

    "contain a deletion message" in {
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("deletion-message").text shouldBe Messages("deletion.message")
    }

    "contain a button to choose something else to do" in {
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
    }

    "contain a button to choose something else to do which points to what do you want to do page" in {
      val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
      doc(result).getElementById("choose-something-else").attr("href") should include("/what-do-you-want-to-do")
    }

    "download a file containing the results" in {
      val result = await(TestWhatDoYouWantToDoController.getResultsFile("testFile.csv").apply(
        FakeRequest(Helpers.GET, "/whatDoYouWantToDo/results/:testFile.csv")))
      contentAsString(result) shouldBe row1
    }

    "redirect to error page" when {

      "render upload result page is called but a file session does not exist" in {
        when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(None))
        val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }

      "render upload result page is called but results file does not exist" in {
        val fileSession = FileSession(None,None,"1234",None)
        when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
        val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }

      "render upload result page is called but there is no callback data in the retrieved file session" in {
        val fileSession = FileSession(None,Some(ResultsFileMetaData("",None,None,1,1L)),"1234",Some(new DateTime().plusDays(10).getMillis))
        when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
        val result = await(TestWhatDoYouWantToDoController.renderUploadResultsPage(fakeRequest))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("/global-error")
      }
    }
  }



}