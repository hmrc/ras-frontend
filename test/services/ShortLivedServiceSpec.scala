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

package services

import models.FileUploadStatus._
import models.{CallbackData, FileSession, ResultsFileMetaData}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import org.scalatest.WordSpecLike
import utils.RasTestHelper

import scala.concurrent.Future

class ShortLivedServiceSpec extends WordSpecLike with RasTestHelper {
  val fileId = "file-id-1"
  val fileStatus = "AVAILABLE"
  val reason: Option[String] = None
  val callbackData = CallbackData("1234", fileId, fileStatus, reason)
  val resultsFile = ResultsFileMetaData(fileId,Some("fileName.csv"),Some(1234L),123,1234L)
  val fileSession = FileSession(Some(callbackData), Some(resultsFile), "userId", Some(DateTime.now().getMillis), None)
  val fileSession1 = FileSession(Some(callbackData), None, "userId", Some(DateTime.now().minusDays(2).getMillis), None)
  val fileSession2 = FileSession(Some(callbackData), None, "userId2", Some(DateTime.now().minusHours(2).getMillis), None)


  val json: JsValue = Json.toJson(fileSession)

	val TestShortLivedCache: ShortLivedCache = new ShortLivedCache(mockRasShortLivedHttpCache, mockAppConfig, mockAppCrypto) {

    when(mockRasShortLivedHttpCache.remove(any())(any(), any())).thenReturn(Future.successful(HttpResponse(200, "")))

    when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())
      (any(),any(), any()))
      .thenReturn(Future.successful(Some(fileSession)))

    when(mockRasShortLivedHttpCache.cache[FileSession] (any(), any(),any(),any())
      (any[Writes[FileSession]], any[HeaderCarrier], any()))
      .thenReturn(Future.successful(CacheMap("sessionValue", Map("1234" -> json))))
  }

  "ShortLivedService" must {
    "cache fileSession in sav4later" in {
      val res = await(TestShortLivedCache.createFileSession("1234","56789"))
      res shouldBe true
    }
    "return false on failing to cache fileSession data" in {
      when(mockRasShortLivedHttpCache.cache[FileSession] (any(), any(),any(),any())(any[Writes[FileSession]], any[HeaderCarrier], any())).thenReturn(Future.failed(new Exception))
      val res = await(TestShortLivedCache.createFileSession("1234","56789"))
      res shouldBe false
    }
    "should get cached fileSession from ShortLivedCache" in {
      val res = await(TestShortLivedCache.fetchFileSession("1234"))
      res.get shouldBe fileSession
    }
    "return none on failing to get cached data" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.failed(new Exception))
      val res = await(TestShortLivedCache.fetchFileSession("1234"))
      res shouldBe None
    }
    " return false if a file is not uploaded by the user" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(None))

      val res = await(TestShortLivedCache.isFileInProgress("userId"))
      res shouldBe false
    }

    " return true a file is uploaded before and is in progress" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession)))

      val res = await(TestShortLivedCache.isFileInProgress("userId"))
      res shouldBe true
    }
    "return true if a results file is available in fileSession" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession)))

      val res = await(TestShortLivedCache.isFileInProgress("userId"))
      res shouldBe true
    }
    "return false if a file uploaded time is more than 24 hours and no results file" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession1)))

      val res = await(TestShortLivedCache.isFileInProgress("userId"))
      res shouldBe false
    }
    "return true if a file uploaded time is less than 24 hours and no results file" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession2)))

      val res = await(TestShortLivedCache.isFileInProgress("userId2"))
      res shouldBe true
    }
    "return false failing to get fileSession to check if the file is in progress" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.failed(new Exception))
      val res = await(TestShortLivedCache.isFileInProgress("userId2"))
      res shouldBe false
    }
    "removes fileSession from cache" in {
      when(mockRasShortLivedHttpCache.remove ("56789")).thenReturn(Future.successful(HttpResponse(202, "")))
      val res = await(TestShortLivedCache.removeFileSessionFromCache("56789"))
      res shouldBe 202
    }

    "return the correct file status" when {
      "file session does not exist" in {
        when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(), any(), any())).thenReturn(Future.successful(None))

        val res = await(TestShortLivedCache.determineFileStatus("userId"))
        res shouldBe NoFileSession
      }

      "file session exists and file is ready" in {
        when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(), any(), any())).thenReturn(Future.successful(Some(fileSession)))

        val res = await(TestShortLivedCache.determineFileStatus("userId"))
        res shouldBe Ready
      }

      "file session exists and file is in progress" in {
        when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(), any(), any())).thenReturn(Future.successful(Some(fileSession2)))

        val res = await(TestShortLivedCache.determineFileStatus("userId"))
        res shouldBe InProgress

      }
      "file session exists and more then 24 hours has passed" in {
        when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(), any(), any())).thenReturn(Future.successful(Some(fileSession1)))

        val res = await(TestShortLivedCache.determineFileStatus("userId"))
        res shouldBe TimeExpiryError

      }

      "file session exists and there is a problem with the file upload process" in {
        val cd = callbackData.copy(status = "ERROR")
        val fileSession = FileSession(Some(cd), None, "userId", Some(DateTime.now().minusHours(2)getMillis), None)
        when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession)))

        val res = await(TestShortLivedCache.determineFileStatus("userId"))
        res shouldBe UploadError
      }

    }
  }

  "failedProcessingUploadedFile" must {

    "return true if it has been 24 hours since upload and no results file exists" in {
      val fileSession = FileSession(Some(callbackData), None, "userId", Some(DateTime.now().minusDays(2)getMillis), None)
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val res = await(TestShortLivedCache.failedProcessingUploadedFile("userId"))
      res shouldBe true
    }

    "return false if it hasn't been 24 hours since upload and no results file exists" in {
      val fileSession = FileSession(Some(callbackData), None, "userId2", Some(DateTime.now().minusHours(2)getMillis), None)
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val res = await(TestShortLivedCache.failedProcessingUploadedFile("userId"))
      res shouldBe false
    }

    "return false if no upload timestamp has been found" in {
      val fileSession = FileSession(Some(callbackData), Some(resultsFile), "userId", None, None)
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(Some(fileSession)))
      val res = await(TestShortLivedCache.failedProcessingUploadedFile("userId"))
      res shouldBe false
    }

    "return false if no file session has been found" in {
      when(mockRasShortLivedHttpCache.fetchAndGetEntry[FileSession] (any(), any(),any())(any(),any(), any())).thenReturn(Future.successful(None))
      val res = await(TestShortLivedCache.failedProcessingUploadedFile("userId"))
      res shouldBe false
    }

  }
}
