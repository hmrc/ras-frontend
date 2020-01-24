/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import helpers.RandomNino
import models._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier


class SessionServiceSpec extends UnitSpec with OneServerPerSuite with ScalaFutures with MockitoSugar {

  val mockSessionCache = mock[SessionCache]
  val mockConfig = mock[ApplicationConfig]

  val name = MemberName("John", "Johnson")
  val nino = MemberNino(RandomNino.generate)
  val memberDob = MemberDateOfBirth(RasDate(Some("12"),Some("12"), Some("2012")))
  val memberDetails = MemberDetails(name,RandomNino.generate,RasDate(Some("1"),Some("1"),Some("1999")))
  val uploadResponse = UploadResponse("111",Some("error error"))
  val envelope = Envelope("someEnvelopeId1234")
  val rasSession = RasSession(name,nino,memberDob,None)

  implicit val headerCarrier = HeaderCarrier()

  object TestSessionService extends SessionService {
    override def sessionCache: SessionCache = mockSessionCache
    override val config: ApplicationConfig = mockConfig
  }

  "Session service" should {
    
    "cache Name" when {
      "no session is retrieved" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(),any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(name = name))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheName(name)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(name = name))
      }
      "some session is retrieved" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(name = name))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheName(name)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(name = name))
      }
      "set to a clean value" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(name = TestSessionService.cleanMemberName))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheName()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(name = TestSessionService.cleanMemberName))
      }
    }

    "cache residency status" when {
      "member details is submitted via the form when no returned session" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val rd = ResidencyStatusResult("uk",Some("uk"),"2000","2001","Jack","1-1-1999",RandomNino.generate)
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = Some(rd)))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheResidencyStatusResult(rd)(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = Some(rd)))
      }
      "member details is submitted via the form when some returned session" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val rd = ResidencyStatusResult("uk",Some("uk"),"2000","2001","Jack","1-1-1999",RandomNino.generate)
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = Some(rd)))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheResidencyStatusResult(rd)(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = Some(rd)))
      }
      "member details is submitted via the form and the date is in the period where only CY is returned" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val rd = ResidencyStatusResult("uk",None,"2000","2001","Jack","1-1-1999",RandomNino.generate)
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = Some(rd)))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheResidencyStatusResult(rd)(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = Some(rd)))
      }
      "set to an empty value" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(residencyStatusResult = None))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheResidencyStatusResult()(HeaderCarrier()), 10 seconds)
        result shouldBe Some(rasSession.copy(residencyStatusResult = None))
      }
    }

    "cache nino" when {
      "member details is submitted via the form when no returned session" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(nino = nino))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheNino(nino)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(nino = nino))
      }
      "member details is submitted via the form when some returned session" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(nino = nino))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheNino(nino)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(nino = nino))
      }
      "set to an empty value" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(nino = TestSessionService.cleanMemberNino))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheNino()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(nino = TestSessionService.cleanMemberNino))
      }
    }

    "cache dob" when {
      "member details is submitted via the form when no returned session" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(dateOfBirth = memberDob))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheDob(memberDob)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(dateOfBirth = memberDob))
      }
      "member details is submitted via the form when some returned session" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(dateOfBirth = memberDob))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheDob(memberDob)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(dateOfBirth = memberDob))
      }
      "set to an empty value" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(dateOfBirth = TestSessionService.cleanMemberDateOfBirth))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheDob()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(dateOfBirth = TestSessionService.cleanMemberDateOfBirth))
      }
    }

    "cache file upload response" when {
      "no session is available" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = Some(uploadResponse)))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheUploadResponse(uploadResponse)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = Some(uploadResponse)))
      }
      "a session is available" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = Some(uploadResponse)))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheUploadResponse(uploadResponse)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = Some(uploadResponse)))
      }
      "set to an empty value" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = None))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheUploadResponse()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = None))
      }
    }

    "return file upload session without upload response" when {
      "an error occurs on the file upload page" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(uploadResponse = None))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheUploadResponse(uploadResponse)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(uploadResponse = None))
      }
    }

    "cache envelope" when {
      "no session is available" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](rasSession.copy(envelope = Some(envelope)))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheEnvelope(envelope)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(envelope = Some(envelope)))
      }
      "a session is available" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(envelope = Some(envelope)))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.cacheEnvelope(envelope)(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(envelope = Some(envelope)))
      }
      "set to an empty value" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val json = Json.toJson[RasSession](rasSession.copy(envelope = None))
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetCacheEnvelope()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession.copy(envelope = None))
      }
    }

    "fetch ras session" when {
      "requested" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(Some(rasSession)))
        val result = Await.result(TestSessionService.fetchRasSession()(headerCarrier), 10 seconds)
        result shouldBe Some(rasSession)
      }
    }

    "return a clean session" when {
      "reset cache with key all is called" in {
        when(mockSessionCache.fetchAndGetEntry[RasSession](any())(any(), any(), any())).thenReturn(Future.successful(None))
        val json = Json.toJson[RasSession](SessionService.cleanSession)
        when(mockSessionCache.cache[RasSession](any(), any())(any(), any(), any())).thenReturn(Future.successful(CacheMap("sessionValue", Map("ras_session" -> json))))
        val result = Await.result(TestSessionService.resetRasSession()(headerCarrier), 10 seconds)
        result shouldBe Some(SessionService.cleanSession)
      }
    }
  }

  "ShortLivedCache" should {
    "return error in file upload" when {
      "Status is not equal to Available" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None, None)
        ShortLivedCache.errorInFileUpload(fileSession) shouldBe true
      }
    }

    "return the correct filename from getDownloadFileName" when {
      "there is no metadata in session" in {
        val fileSession = FileSession(
          Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None, None)
        ShortLivedCache.getDownloadFileName(fileSession) shouldBe "Residency-status"
      }

      "there is metadata in session but there is no name in metadata" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None,
          Some(FileMetadata("", None, None)))
        ShortLivedCache.getDownloadFileName(fileSession) shouldBe "Residency-status"
      }

      "there is metadata in session and there is a name and extension in session" in {
        val fileSession = FileSession(Some(CallbackData("", "someFileId", "ERROR", None)), None, "1234", None,
          Some(FileMetadata("", Some("originalName.csv"), None)))
        ShortLivedCache.getDownloadFileName(fileSession) shouldBe "originalName"
      }
    }

    "not return error in file upload" when {
      "Status is equal to Available" in {
        val fileSession = FileSession(Some(CallbackData("","someFileId","AVAILABLE",None)),None,"1234",None,None)
        ShortLivedCache.errorInFileUpload(fileSession) shouldBe false
      }
      "File Session is empty" in {
        val fileSession = FileSession(None,None,"1234",None,None)
        ShortLivedCache.errorInFileUpload(fileSession) shouldBe false
      }
    }
  }
}
