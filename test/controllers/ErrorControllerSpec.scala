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

import config.ApplicationConfig
import helpers.helpers.I18nHelper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import connectors.UserDetailsConnector
import models._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

class ErrorControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar with I18nHelper {

  val fakeRequest = FakeRequest("GET", "/")
  val mockAuthConnector = mock[AuthConnector]
  val mockUserDetailsConnector = mock[UserDetailsConnector]
  val mockConfig = mock[Configuration]
  val mockEnvironment = mock[Environment]
  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")
  private val enrolment = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
  private val enrolments = new Enrolments(Set(enrolment))
  val successfulRetrieval: Future[Enrolments] = Future.successful(enrolments)

  object TestErrorController extends ErrorController {
    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
    when(mockUserDetailsConnector.getUserDetails(any())(any())).thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))
  }

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  "ErrorController" should {

    "respond to GET /relief-at-source/not-authorised" in {
      val result = await(route(fakeApplication, FakeRequest(GET, "/relief-at-source/not-authorised")))
      status(result.get) should not equal (NOT_FOUND)
    }

    "return error when global error endpoint is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return error when problem getting results is called" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return error when problem uploading file results is called" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 200 when not authorised file results is called" in {
      val result = TestErrorController.notAuthorised(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return error when file not available is called" in {
      val result = TestErrorController.fileNotAvailable(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 200 when render No Result Available Page is called" in {
      val result = TestErrorController.renderNoResultAvailablePage(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return 200 when render No Results Available Yet Page is called" in {
      val result = TestErrorController.renderNoResultsAvailableYetPage(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML when global error is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "return HTML when upload error is called" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

    "return HTML when problem uploading file is called" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }
  }

  "global error page" should {

    "contain correct title and header" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("global.error.page.title")
      doc.getElementById("header").text shouldBe Messages("global.error.header")
      doc.getElementById("message").text shouldBe Messages("global.error.message")
    }
  }

  "problem getting results page" should {

    "contain correct title and header" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("problem.getting.results.title")
      doc.getElementById("back").attr("href") should include("/")
      doc.getElementById("header").text shouldBe Messages("problem.getting.results.header")
      doc.getElementById("try-again").text shouldBe Messages("check.upload.file.again", Messages("file.formatted.correctly"))
      doc.getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
    }

    "contain correct ga events" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("back").attr("data-journey-click") shouldBe "navigation - link:There has been a problem getting your results:Back"
      doc.getElementById("choose-something-else").attr("data-journey-click") shouldBe "button - click:There has been a problem getting your results:Choose something else to do"
      doc.getElementById("file-formatting").attr("data-journey-click") shouldBe "link - click:There has been a problem getting your results:Check file format"
    }
  }

  "problem uploading file page" should {

    "contain correct title and header when problem uploading file" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("problem.uploading.file.title")
      doc.getElementById("back").attr("href") should include("/upload-a-file")
      doc.getElementById("header").text shouldBe Messages("problem.uploading.file.header")
      doc.getElementById("try-again").text shouldBe Messages("upload.file.again")
      doc.getElementById("check-file").text shouldBe Messages("check.file")
      doc.getElementById("return-to-upload").text shouldBe Messages("return.to.upload")
    }

    "contain correct ga events when problem uploading file" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("back").attr("data-journey-click") shouldBe "navigation - link:There has been a problem uploading your file:Back"
      doc.getElementById("return-to-upload").attr("data-journey-click") shouldBe "button - click:There has been a problem uploading your file:Return to upload a file"
    }
  }

  "file not available page" should {

    "contain the correct page title" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).title shouldBe Messages("file.not.available.page.title")
    }

    "contain the correct page header" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("header").text shouldBe Messages("file.not.available.page.header")
    }

    "contain a back link pointing to /relief-at-source" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("back").attr("href") shouldBe ("/relief-at-source")
    }

    "contain the correct content paragraph" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("sub-header").text shouldBe Messages("file.not.available.sub-header", Messages("file.not.available.link"))
    }

    "contain the correct link in the content paragraph" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("sub-header-link").attr("href") shouldBe ("/relief-at-source")
    }

    "contain the correct ga events" in {
      val result = await(TestErrorController.fileNotAvailable(fakeRequest))
      doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:File not available:Back"
      doc(result).getElementById("sub-header-link").attr("data-journey-click") shouldBe "link - click:File not available:Choose something else to do"
    }
  }

  "not authorised page" should {
    "contain the correct page title" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).title shouldBe Messages("unauthorised.error.page.title")
    }

    "contain the correct page header" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("header").text shouldBe Messages("unauthorised.page.header")
    }

    "contain the correct top paragraph" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("paragraph-top").text shouldBe Messages("unauthorised.paragraph.top")
    }

    "contain the correct bottom paragraph" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("paragraph-bottom").text shouldBe Messages("unauthorised.paragraph.bottom")
    }

    "contain a list with two items" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("action-list").children().size() shouldBe 2
    }

    "first list item should contain the correct text" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("action-list").children().first().text shouldBe Messages("unauthorised.list.first", Messages("unauthorised.list.first.link"))
    }

    "first list item link should have the correct href" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-sign-in").attr("href") shouldBe ApplicationConfig.signOutAndContinueUrl
    }

    "second list item should contain the correct text" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("action-list").children().last().text shouldBe Messages("unauthorised.list.last")
    }

    "second list item link should have the correct href" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-register").attr("href") shouldBe "https://online.hmrc.gov.uk/registration/pensions"
    }

    "first list item link should have the correct ga event" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-sign-in").attr("data-journey-click") shouldBe "link - click:There is a problem:Sign in"
    }

    "second list item link should have the correct ga event" in {
      val result = await(TestErrorController.notAuthorised(fakeRequest))
      doc(result).getElementById("link-register").attr("data-journey-click") shouldBe "link - click:There is a problem:Register"
    }
  }

  "renderResultsNotAvailableYetPage" should {
    "contain the correct page title" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).title shouldBe Messages("results.not.available.yet.page.title")
    }

    "contain the correct page header" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).getElementById("header").text shouldBe Messages("results.not.available.yet.page.header")
    }

    "contain the correct sub header 1" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).getElementById("sub-header1").text shouldBe Messages("results.not.available.yet.sub-header1")
    }

    "contain the correct sub header 2" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).getElementById("sub-header2").text shouldBe Messages("results.not.available.yet.sub-header2")
    }

    "contain a back link pointing to /" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).getElementById("back").attr("href") should include("/")
    }

    "contain a choose something else to do button" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
    }

    "contain a choose something else to do button that points to /" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).getElementById("choose-something-else").attr("href") should include ("/")
    }

    "contain the correct ga events" in {
      val result = await(TestErrorController.renderNoResultsAvailableYetPage(fakeRequest))
      doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:Results are still being added:Back"
      doc(result).getElementById("choose-something-else").attr("data-journey-click") shouldBe "button - click:Results are still being added:Choose something else to do"
    }

  }

  "renderNoResultsAvailablePage" should {
    "contain the correct page title" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).title shouldBe Messages("no.results.available.page.title")
    }

    "contain the correct page header" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).getElementById("header").text shouldBe Messages("no.results.available.page.header")
    }

    "contain the correct page content" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).getElementById("sub-header").text shouldBe Messages("no.results.available.sub-header", Messages("no.results.available.link"))
    }

    "contain a back link pointing to /" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).getElementById("back").attr("href") should include("/")
    }

    "contain a choose something else to do button" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
    }

    "contain a choose something else to do button that points to /" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).getElementById("choose-something-else").attr("href") should include ("/")
    }

    "contain a link back to the upload a file page" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).getElementById("upload-link").attr("href") should include("/upload-a-file")
    }

    "contain the correct ga events" in {
      val result = await(TestErrorController.renderNoResultAvailablePage(fakeRequest))
      doc(result).getElementById("choose-something-else").attr("data-journey-click") shouldBe "button - click:You have not uploaded a file:Choose something else to do"
      doc(result).getElementsByClass("link-back").attr("data-journey-click") shouldBe "navigation - link:You have not uploaded a file:Back"
      doc(result).getElementById("upload-link").attr("data-journey-click") shouldBe "link - click:You have not uploaded a file:Upload a file"
    }
  }
}
