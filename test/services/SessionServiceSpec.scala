/*
 * Copyright 2021 HM Revenue & Customs
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

import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{RandomNino, RasTestHelper}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class SessionServiceSpec extends UnitSpec with RasTestHelper {

  val name: MemberName = MemberName("John", "Johnson")
  val nino: MemberNino = MemberNino(RandomNino.generate)
  val memberDob: MemberDateOfBirth = MemberDateOfBirth(RasDate(Some("12"),Some("12"), Some("2012")))
  val memberDetails: MemberDetails = MemberDetails(name,RandomNino.generate,RasDate(Some("1"),Some("1"),Some("1999")))
  val uploadResponse: UploadResponse = UploadResponse("111",Some("error error"))
  val envelope: Envelope = Envelope("someEnvelopeId1234")
  val rasSession: RasSession = RasSession(name,nino,memberDob,None)

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val TestSessionService: SessionService = new SessionService(mockHttp, mockRasSessionCache, mockAppConfig)
	val TestShortLivedCache = new ShortLivedCache(mockRasShortLivedHttpCache, mockAppConfig, mockAppCrypto)


	"Session service" should {

    "cache Name" when {
      "no session is retrieved" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(),any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(name = name))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheName(name)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(name = name))
      }
      "some session is retrieved" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(name = name))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheName(name)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(name = name))
      }
      "set to a clean value" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(name = TestSessionService.cleanMemberName))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheName()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(name = TestSessionService.cleanMemberName))
      }
    }

    "cache residency status" when {
      "member details is submitted via the form when no returned session" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val rd = ResidencyStatusResult("uk",Some("uk"),"2000","2001","Jack","1-1-1999",RandomNino.generate)
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = Some(rd)))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheResidencyStatusResult(rd)(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = Some(rd)))
      }
      "member details is submitted via the form when some returned session" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val rd = ResidencyStatusResult("uk",Some("uk"),"2000","2001","Jack","1-1-1999",RandomNino.generate)
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = Some(rd)))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheResidencyStatusResult(rd)(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = Some(rd)))
      }
      "member details is submitted via the form and the date is in the period where only CY is returned" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val rd = ResidencyStatusResult("uk",None,"2000","2001","Jack","1-1-1999",RandomNino.generate)
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = Some(rd)))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheResidencyStatusResult(rd)(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = Some(rd)))
      }
      "set to an empty value" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = None))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheResidencyStatusResult()(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = None))
      }
    }

    "cache nino" when {
      "member details is submitted via the form when no returned session" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(nino = nino))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheNino(nino)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(nino = nino))
      }
      "member details is submitted via the form when some returned session" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(nino = nino))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheNino(nino)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(nino = nino))
      }
      "set to an empty value" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(nino = TestSessionService.cleanMemberNino))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheNino()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(nino = TestSessionService.cleanMemberNino))
      }
    }

    "cache dob" when {
      "member details is submitted via the form when no returned session" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(dateOfBirth = memberDob))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheDob(memberDob)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(dateOfBirth = memberDob))
      }
      "member details is submitted via the form when some returned session" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(dateOfBirth = memberDob))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheDob(memberDob)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(dateOfBirth = memberDob))
      }
      "set to an empty value" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(dateOfBirth = TestSessionService.cleanMemberDateOfBirth))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheDob()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(dateOfBirth = TestSessionService.cleanMemberDateOfBirth))
      }
    }

    "cache file upload response" when {
      "no session is available" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = Some(uploadResponse)))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheUploadResponse(uploadResponse)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = Some(uploadResponse)))
      }
      "a session is available" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = Some(uploadResponse)))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheUploadResponse(uploadResponse)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = Some(uploadResponse)))
      }
      "set to an empty value" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = None))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheUploadResponse()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = None))
      }
    }

    "return file upload session without upload response" when {
      "an error occurs on the file upload page" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = None))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheUploadResponse(uploadResponse)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = None))
      }
    }

    "cache envelope" when {
      "no session is available" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(envelope = Some(envelope)))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheEnvelope(envelope)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(envelope = Some(envelope)))
      }
      "a session is available" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(envelope = Some(envelope)))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheEnvelope(envelope)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(envelope = Some(envelope)))
      }
      "set to an empty value" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(envelope = None))
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheEnvelope()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(envelope = None))
      }
    }

    "fetch ras session" when {
      "requested" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val result = Await.result(TestSessionService.fetchRasSession()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession)
      }
    }

    "return a clean session" when {
      "reset cache with key all is called" in {
        when(mockRasSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](TestSessionService.cleanSession)
        when(mockRasSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetRasSession()(headerCarrier), 10 seconds)
        result shouldBe Some(TestSessionService.cleanSession)
      }
    }
  }

  "ShortLivedCache" should {
    "return error in file upload" when {
      "Status is not equal to Available" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None, None)

				when(mockRasShortLivedHttpCache.remove(any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))
				TestShortLivedCache.errorInFileUpload(fileSession) shouldBe true

				verify(mockRasShortLivedHttpCache, times(1)).remove(any())(any(), any())
			}
    }

    "return the correct filename from getDownloadFileName" when {
      "there is no metadata in session" in {
        val fileSession = FileSession(
          Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None, None)
        TestShortLivedCache.getDownloadFileName(fileSession) shouldBe "Residency-status"
      }

      "there is metadata in session but there is no name in metadata" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None,
          Some(FileMetadata("", None, None)))
        TestShortLivedCache.getDownloadFileName(fileSession) shouldBe "Residency-status"
      }

      "there is metadata in session and there is a name and extension in session" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None,
          Some(FileMetadata("", Some("originalName.csv"), None)))
        TestShortLivedCache.getDownloadFileName(fileSession) shouldBe "originalName"
      }
    }

    "not return error in file upload" when {
      "Status is equal to Available" in {
        val fileSession = FileSession(Some(CallbackData("","someFileId","AVAILABLE",None)),None,"1234",None,None)
        TestShortLivedCache.errorInFileUpload(fileSession) shouldBe false
      }
      "File Session is empty" in {
        val fileSession = FileSession(None,None,"1234",None,None)
        TestShortLivedCache.errorInFileUpload(fileSession) shouldBe false
      }
    }
  }
}
