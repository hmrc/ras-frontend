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

package services

import models.{CallbackData, FileSession, ResultsFileMetaData}
import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedHttpCaching}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ShortLivedServiceSpec extends UnitSpec with OneServerPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val fileId = "file-id-1"
  val fileStatus = "AVAILABLE"
  val reason: Option[String] = None
  val callbackData = CallbackData("1234", fileId, fileStatus, reason)
  val resultsFile = ResultsFileMetaData(fileId,Some("fileName.csv"),Some(1234L),123,1234L)
  val fileSession = FileSession(Some(callbackData), Some(resultsFile), "userId", Some(DateTime.now().getMillis))
  val json = Json.toJson(fileSession)

  val mockSessionCache = mock[ShortLivedHttpCaching]
  val SUT = new ShortLivedCache {
    override val shortLivedCache: ShortLivedHttpCaching = mockSessionCache
    when(shortLivedCache.fetchAndGetEntry[FileSession] (any(), any(),any())
      (any(),any(), any()))
      .thenReturn(Future.successful(Some(fileSession)))

    when(shortLivedCache.cache[FileSession] (any(), any(),any(),any())
      (any[Writes[FileSession]], any[HeaderCarrier], any()))
      .thenReturn(Future.successful(CacheMap("sessionValue", Map("1234" -> json))))
  }

  "ShortLivedService" should {
    "cache fileSession in sav4later" in {
      val results = List("Nino, firstName, lastName, dob, cyResult, cy+1Result")
      val res = await(SUT.createFileSession("1234","56789"))
      res shouldBe true
    }
    "return false on caching fileSession data" in {
      when(mockSessionCache.cache[FileSession] (any(), any(),any(),any())
        (any[Writes[FileSession]], any[HeaderCarrier], any()))
        .thenReturn(Future.failed(new Exception))
      val res = await(SUT.createFileSession("1234","56789"))
      res shouldBe false
    }
    "should get cached fileSession from ShortLivedCache" in {
      val res = await(SUT.fetchFileSession("1234"))
      res.get shouldBe fileSession
    }
    "return none on failing to get cached data" in {
      when(mockSessionCache.fetchAndGetEntry[FileSession] (any(), any(),any())
        (any(),any(), any()))
        .thenReturn(Future.failed(new Exception))
      val res = await(SUT.fetchFileSession("1234"))
      res shouldBe None
    }
    }
}
