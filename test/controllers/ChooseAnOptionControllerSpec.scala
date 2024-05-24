/*
 * Copyright 2023 HM Revenue & Customs
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

import models._
import org.joda.time.{DateTime, LocalDate}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, include}
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Result
import play.api.test.Helpers.{OK, contentAsString, _}
import play.api.test.{FakeRequest, Helpers}
import services.TaxYearResolver
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import utils.RasTestHelper

import java.io.ByteArrayInputStream
import scala.concurrent.Future


class ChooseAnOptionControllerSpec extends AnyWordSpec with RasTestHelper {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  val currentTaxYear: Int = TaxYearResolver.currentTaxYear

  private val enrolmentIdentifier = EnrolmentIdentifier("PSAID", "Z123456")
  private val enrolment = new Enrolment(key = "HMRC-PSA-ORG", identifiers = List(enrolmentIdentifier), state = "Activated")
  val successfulRetrieval: Future[Enrolments] = Future.successful(Enrolments(Set(enrolment)))
  val mockUploadTimeStamp: Long = new DateTime().minusDays(10).getMillis
  val mockExpiryTimeStamp: Long = new DateTime().minusDays(7).getMillis
  val mockResultsFileMetadata: ResultsFileMetaData = ResultsFileMetaData("",Some("testFile.csv"),Some(mockUploadTimeStamp),1,1L)
  val fileSession: FileSession = FileSession(Some(CallbackData("",None,"",None, None)),Some(mockResultsFileMetadata),"1234",Some(DateTime.now().getMillis),None)

  val row1 = "John,Smith,AB123456C,1990-02-21"
  val inputStream = new ByteArrayInputStream(row1.getBytes)

  val TestChooseAnOptionController: ChooseAnOptionController = new ChooseAnOptionController(mockResidencyStatusAPIConnector, mockAuthConnector, mockFilesSessionService, mockMCC, mockAppConfig, chooseAnOptionView, fileReadyView, uploadResultView, resultsNotAvailableYetView, noResultsAvailableView) {

    when(mockFilesSessionService.determineFileStatus(any())(any(), any())).thenReturn(Future.successful(FileUploadStatus.NoFileSession))
    when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any())).thenReturn(successfulRetrieval)
    when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
    when(mockResidencyStatusAPIConnector.getFile(any(), any())(any(), any())).thenReturn(Future.successful(Some(inputStream)))
    when(mockUserDetailsConnector.getUserDetails(any())(any(), any())).thenReturn(Future.successful(UserDetails(None, None, "", groupIdentifier = Some("group"))))
  }

  "getHelpDate" must {
    import models.FileUploadStatus._
    val testTimeStamp: Long = new LocalDate(2013, 4, 5).toDate.getTime
    val currentTime = new DateTime(2014, 4, 6, 0, 0, 0, 0)
    val mockResultsFileMetadata = ResultsFileMetaData("",Some("testFile.csv"),Some(testTimeStamp),1,1L)
    val optionalFileSession = Some(FileSession(Some(CallbackData("",None,"",None,None)),Some(mockResultsFileMetadata),"1234",Some(currentTime.getMillis),None))

    "return the expiry date format message" in {
      val result = TestChooseAnOptionController.getHelpDate(Ready, optionalFileSession)
     result shouldBe Some("12:00am on Monday 8 April 2013")
    }

    "return the upload date format message" in {
      val result = TestChooseAnOptionController.getHelpDate(InProgress, optionalFileSession)
      result shouldBe Some("yesterday at 12:00am")
    }

    "return None when there fileStatus is not Ready or InProgress" in {
      val result = TestChooseAnOptionController.getHelpDate(NoFileSession, optionalFileSession)
      result shouldBe None
    }

    "return None when there is no File session" in {
      val result = TestChooseAnOptionController.getHelpDate(Ready, None)
      result shouldBe None
    }
  }

  "get" when {

    "for any status" must {
      "respond to GET /choose-an-option-to-get-residency-status" in {
        val result: Future[Result] = TestChooseAnOptionController.get(fakeRequest)
        status(result) shouldBe OK
      }
    }
  }

  "renderUploadResultsPage" must {
    "return ok when called" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result: Result = await(TestChooseAnOptionController.renderUploadResultsPage(fakeRequest))
      result.header.status shouldBe OK
    }

    "return global error" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession.copy(userFile = None))))
      val result: Future[Result] = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
      redirectLocation(result) should include("/global-error")
    }

		"download a file containing the results" in {
			val mockUploadTimeStamp = DateTime.parse("2018-12-31").getMillis
			val mockResultsFileMetadata = ResultsFileMetaData("",Some("testFile.csv"),Some(mockUploadTimeStamp),1,1L)
			val fileSession = FileSession(Some(CallbackData("",None,"",None, None)),Some(mockResultsFileMetadata),"1234",None,None)
			when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
			val result = TestChooseAnOptionController.getResultsFile("testFile.csv").apply(
				FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
			contentAsString(result) shouldBe row1
		}

    "not be able to download a file containing the results when file name is incorrect" in {
      val mockUploadTimeStamp = DateTime.parse("2018-12-31").getMillis
      val mockResultsFileMetadata = ResultsFileMetaData("",Some("wrongName.csv"),Some(mockUploadTimeStamp),1,1L)
      val fileSession = FileSession(Some(CallbackData("",None,"",None,None)),Some(mockResultsFileMetadata),"1234",None,None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.getResultsFile("testFile.csv").apply(
        FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/file-not-available")
    }

    "not be able to download a file containing the results when there is no results file" in {
      val fileSession = FileSession(Some(CallbackData("",None,"",None,None)),None,"1234",None,None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.getResultsFile("testFile.csv").apply(
        FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/file-not-available")
    }

    "not be able to download a file containing the results when there is no file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.getResultsFile("testFile.csv").apply(
        FakeRequest(Helpers.GET, "/chooseAnOption/results/:testFile.csv"))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/file-not-available")
    }

    "redirect to error page" when {

      "render upload result page is called but there is no callback data in the retrieved file session" in {
        val fileSession = FileSession(None,Some(ResultsFileMetaData("",None,None,1,1L)),"1234",Some(new DateTime().plusDays(10).getMillis),None)
        when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
        val result = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should include("/global-error")
      }
    }
  }

  "renderFileReadyPage" must {
    "return ok when called" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderFileReadyPage(fakeRequest)
      status(result) shouldBe OK
    }

    "return global error page when there is no file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.renderFileReadyPage(fakeRequest)
      redirectLocation(result) should include("/global-error")
    }

    "redirect to cannot upload another file there is no result file ready" in {
      val fileSession = FileSession(None, None, "1234", None, None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderFileReadyPage(fakeRequest)
      redirectLocation(result) should include("/cannot-upload-another-file")
    }
  }

  "renderResultsNotAvailableYetPage" must {
    "return ok when called" in {
      val fileSession = FileSession(None,None,"1234",None,None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderUploadResultsPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/results-not-available")
    }

    "return error when there is a result file in file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderNoResultsAvailableYetPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/residency-status-added")
    }

    "return error when there is no file session" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.renderNoResultsAvailableYetPage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/no-results-available")
    }
  }

  "renderNoResultAvailablePage" must {
    "return ok when called" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(None))
      val result = TestChooseAnOptionController.renderNoResultAvailablePage(fakeRequest)
      status(result) shouldBe OK
      await(await(result).body.consumeData).utf8String should include("You have not uploaded a file")
    }

    "redirect to results-not-avilable when there is a file session with a file in progress" in {
      val session = fileSession.copy(resultsFile = None)
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(session)))
      val result = TestChooseAnOptionController.renderNoResultAvailablePage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/results-not-available")
    }

    "redirect to results page when there is a file session with a file ready" in {
      when(mockFilesSessionService.fetchFileSession(any())(any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val result = TestChooseAnOptionController.renderNoResultAvailablePage(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) should include("/residency-status-added")
    }
  }

  "fomattedExpiryDate method" must {
    "return correctly formatted date and time" in {
      val date = new DateTime()
        .withDate(2020,3,20)
        .withTime(10,30,0,0)
      assert(TestChooseAnOptionController.formattedExpiryDate(date.getMillis) ==  "10:30am on Monday 23 March 2020"
      )
    }
  }
}
