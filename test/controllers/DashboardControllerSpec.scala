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
import helpers.helpers.I18nHelper
import models.{CallbackData, FileSession, UserDetails}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.mvc.Result
import play.api.test.Helpers.{OK, contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import play.api.{Configuration, Environment}
import services.{SessionService, ShortLivedCache}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


class DashboardControllerSpec extends UnitSpec with OneServerPerSuite with MockitoSugar with I18nHelper {

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
  val fileSession = FileSession(Some(CallbackData("","someFileId","",None)),None,"1234",None)

  val row1 = "John,Smith,AB123456C,1990-02-21"
  val inputStream = new ByteArrayInputStream(row1.getBytes)

  object TestDashboardController extends DashboardController {

    val authConnector: AuthConnector = mockAuthConnector
    override val userDetailsConnector: UserDetailsConnector = mockUserDetailsConnector
    override val resultsFileConnector: ResidencyStatusAPIConnector = mockRasConnector
    override val sessionService = mockSessionService
    override val shortLivedCache = mockShortLivedCache
    override val config: Configuration = mockConfig
    override val env: Environment = mockEnvironment

    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
    when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))

    when(mockRasConnector.getFile(any())(any())).
      thenReturn(Future.successful(Some(inputStream)))

    when(mockUserDetailsConnector.getUserDetails(any())(any())).
      thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))
  }

  private def doc(result: Future[Result]): Document = Jsoup.parse(contentAsString(result))

  "DashboardController" should {

    "respond to GET /dashboard" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestDashboardController.get(fakeRequest)
      status(result) shouldBe OK
    }

    "contain the correct title and header" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).title shouldBe Messages("dashboard.page.title")
      doc(result).getElementById("header").text shouldBe Messages("dashboard.page.header")
    }

    "contain single lookup link and description" in {

      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("single-lookup-link").text shouldBe Messages("single.lookup.link")
      doc(result).getElementById("single-lookup-description").text shouldBe Messages("single.lookup.description")
    }

    "contain bulk lookup link and description" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("bulk-lookup-link").text shouldBe Messages("bulk.lookup.link")
      doc(result).getElementById("bulk-lookup-description").text shouldBe Messages("bulk.lookup.description")
    }

    "contain recent bulk lookups header and description" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("recent-lookups").text shouldBe Messages("recent.lookups")
      doc(result).getElementById("recent-lookups-description").text shouldBe Messages("recent.lookups.description")
    }

    "contain bulk lookup table" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("recent-lookups-table") should not be null
      doc(result).getElementById("reference-table-header").text shouldBe Messages("reference.table.header")
      doc(result).getElementById("upload-date-table-header").text shouldBe Messages("upload.date.table.header")
      doc(result).getElementById("time-left-table-header").text shouldBe Messages("time.left.table.header")
    }

    "not contain a result link when no file is in progress" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(false))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("result-link") shouldBe null
    }

    "contain a result link when file is in progress" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("result-link") should not be null
    }

    "contain file id in the download results link" in {
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("result-link").attr("href") should include("someFileId")
    }

    "disable results link when no callback data is available" in {
      val fileSession = FileSession(None,None,"1234",None)
      when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
      when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(Some(fileSession)))
      val result = TestDashboardController.get(fakeRequest)
      doc(result).getElementById("result").text shouldBe Messages("result")
    }

    "redirect to global error page" when {
      "no file session is available" in {
        when(mockShortLivedCache.isFileInProgress(any())(any())).thenReturn(Future.successful(true))
        when(mockShortLivedCache.fetchFileSession(any())(any()))thenReturn(Future.successful(None))
        val result = TestDashboardController.get(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).get should include("global-error")
      }
    }

    "get results file" in {
      val result = await(TestDashboardController.getResultsFile("testFile.csv").apply(FakeRequest(Helpers.GET,
        "/dashboard/results/:testFile.csv")))
      val content = contentAsString(result)
      content shouldBe row1
    }

  }

}
