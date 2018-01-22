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

    "return 200 when global error endpoint is called" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "return 200 when problem getting results in called" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
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

    "contain correct title and header when global error" in {
      val result = TestErrorController.renderGlobalErrorPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("global.upload.error.page.title")
      doc.getElementById("header").text shouldBe Messages("global.upload.error.header")
      doc.getElementById("sub-header").text shouldBe Messages("global.upload.error.sub-header")
      doc.getElementById("timeout-bullet").text shouldBe Messages("a.system.timeout")
      doc.getElementById("technical-bullet").text shouldBe Messages("a.technical.timeout")
      doc.getElementById("try-again").text shouldBe Messages("try.uploading.again")
      doc.getElementById("continue").text shouldBe Messages("continue")
    }

    "contain correct title and header when upload error" in {
      val result = TestErrorController.renderProblemGettingResultsPage(fakeRequest)
      val doc = Jsoup.parse(contentAsString(result))
      doc.title shouldBe Messages("problem.getting.results.title")
      doc.getElementById("back").attr("href") should include("/what-do-you-want-to-do")
      doc.getElementById("header").text shouldBe Messages("problem.getting.results.header")
      doc.getElementById("try-again").text shouldBe Messages("upload.file.again")
      doc.getElementById("continue").text shouldBe Messages("continue")
    }
  }

}
