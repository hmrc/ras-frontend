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

import helpers.helpers.I18nHelper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}


import scala.concurrent.Future

class ErrorControllerSpec extends UnitSpec with WithFakeApplication with I18nHelper {

  val fakeRequest = FakeRequest("GET", "/")

  object TestErrorController extends ErrorController

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  "ErrorController" should {

    "respond to GET /relief-at-source/global-error" in {
      val result = await(route(fakeApplication, FakeRequest(GET, "/relief-at-source/global-error")))
      status(result.get) should not equal (NOT_FOUND)
    }

    "respond to GET /relief-at-source/problem-getting-results" in {
      val result = await(route(fakeApplication, FakeRequest(GET, "/relief-at-source/problem-getting-results")))
      status(result.get) should not equal (NOT_FOUND)
    }

    "respond to GET /relief-at-source/file-upload-problem" in {
      val result = await(route(fakeApplication, FakeRequest(GET, "/relief-at-source/file-upload-problem")))
      status(result.get) should not equal (NOT_FOUND)
    }

    "return 200 when global error endpoint is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 200 when problem getting results in called" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 200 when problem uploading file results in called" in {
      val result = TestErrorController.renderProblemUploadingFilePage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
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

    "contain correct title and header when global error" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("global.error.page.title")
      doc.getElementById("header").text shouldBe Messages("global.error.header")
      doc.getElementById("message").text shouldBe Messages("global.error.message")
    }

    "contain correct title and header when upload error" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("problem.getting.results.title")
      doc.getElementById("back").attr("href") should include("/")
      doc.getElementById("header").text shouldBe Messages("problem.getting.results.header")
      doc.getElementById("try-again").text shouldBe Messages("upload.file.again")
      doc.getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
    }

    "contain correct ga events when upload error" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("back").attr("data-journey-click") shouldBe "navigation - link:There has been a problem getting your results:Back"
      doc.getElementById("choose-something-else").attr("data-journey-click") shouldBe "button - click:There has been a problem getting your results:Choose something else to do"
    }

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

}
