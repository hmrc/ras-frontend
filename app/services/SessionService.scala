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

import config.{ApplicationConfig, RasShortLivedHttpCaching, SessionCacheWiring}
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.ShortLivedHttpCaching

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SessionService extends SessionService


trait SessionService extends SessionCacheWiring {

  val RAS_SESSION_KEY = "ras_session"
  val cleanSession = RasSession(MemberName("", ""),
    MemberNino(""),
    MemberDateOfBirth(RasDate(None, None, None)),
    ResidencyStatusResult("", "", "", "", "", "", ""),
    None)

  def fetchRasSession()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {
    sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) map (rasSession => rasSession)
  }

  def resetRasSession()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {
    sessionCache.cache[RasSession](RAS_SESSION_KEY, cleanSession) map (cacheMap => Some(cleanSession))
  }

  def cacheName(name: MemberName)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

    val result = sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) flatMap { currentSession =>
      sessionCache.cache[RasSession](RAS_SESSION_KEY,
        currentSession match {
          case Some(returnedSession) => returnedSession.copy(name = name)
          case None => cleanSession.copy(name = name)
        }
      )
    }

    result.map(cacheMap => {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    })
  }

  def cacheNino(nino: MemberNino)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

    val result = sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) flatMap { currentSession =>
      sessionCache.cache[RasSession](RAS_SESSION_KEY,
        currentSession match {
          case Some(returnedSession) => returnedSession.copy(nino = nino)
          case None => cleanSession.copy(nino = nino)
        }
      )
    }

    result.map(cacheMap => {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    })
  }

  def cacheDob(dob: MemberDateOfBirth)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

    val result = sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) flatMap { currentSession =>
      sessionCache.cache[RasSession](RAS_SESSION_KEY,
        currentSession match {
          case Some(returnedSession) => returnedSession.copy(dateOfBirth = dob)
          case None => cleanSession.copy(dateOfBirth = dob)
        }
      )
    }

    result.map(cacheMap => {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    })
  }

  def cacheResidencyStatusResult(residencyStatusResult: ResidencyStatusResult)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

    val result = sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) flatMap { currentSession =>
      sessionCache.cache[RasSession](RAS_SESSION_KEY,
        currentSession match {
          case Some(returnedSession) => returnedSession.copy(residencyStatusResult = residencyStatusResult)
          case None => cleanSession.copy(residencyStatusResult = residencyStatusResult)
        }
      )
    }

    result.map(cacheMap => {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    })
  }

  def cacheUploadResponse(uploadResponse: UploadResponse)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

    val result = sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) flatMap { currentSession =>
      sessionCache.cache[RasSession](RAS_SESSION_KEY,
        currentSession match {
          case Some(returnedSession) => returnedSession.copy(uploadResponse = Some(uploadResponse))
          case None => cleanSession.copy(uploadResponse = Some(uploadResponse))
        }
      )
    }

    result.map(cacheMap => {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    })
  }

  def cacheEnvelope(envelope: Envelope)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

    val result = sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) flatMap { currentSession =>
      sessionCache.cache[RasSession](RAS_SESSION_KEY,
        currentSession match {
          case Some(returnedSession) => returnedSession.copy(envelope = Some(envelope))
          case None => cleanSession.copy(envelope = Some(envelope))
        }
      )
    }

    result.map(cacheMap => {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    })
  }

}

trait ShortLivedCache  {

  val shortLivedCache: ShortLivedHttpCaching = RasShortLivedHttpCaching
  private val source = "ras"

  val hoursToWaitForReUpload = 24

  def createFileSession(userId: String, envelopeId: String)(implicit hc: HeaderCarrier):Future[Boolean] = {

    shortLivedCache.cache[FileSession](source, userId, "fileSession",
      FileSession(None, None, userId, Some(DateTime.now().getMillis))).map(res => true) recover {
      case ex: Throwable => Logger.error(s"unable to create FileSession to cache => " +
        s"${userId} , envelopeId :${envelopeId},  Exception is ${ex.getMessage}")
        false
      //retry or what is the other option?
    }
  }

  def fetchFileSession(userId: String)(implicit hc: HeaderCarrier) = {
    shortLivedCache.fetchAndGetEntry[FileSession](source, userId, "fileSession").recover {
      case ex: Throwable => Logger.error(s"unable to fetch FileSession from cache => " +
        s"${userId} , Exception is ${ex.getMessage}")
        None
    }
  }

  def isFileInProgress(userId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {

    def uploadTimeDiff(time: Long) = {
      val newTime = new DateTime(time)
      newTime.isBefore(DateTime.now.minusHours(hoursToWaitForReUpload).getMillis)
    }

    fetchFileSession(userId).map(fileSession =>
      fileSession.isDefined match {
        case true =>
          fileSession.get.resultsFile.isDefined || !uploadTimeDiff(fileSession.get.uploadTimeStamp.get)
        case false =>
          Logger.warn("fileSession not defined for " + userId)
          false
    }).recover {
      case ex: Throwable =>
        Logger.error(s"unable to fetch FileSession from cache to check isFileInProgress => ${userId} , Exception is ${ex.getMessage}")
        false
    }
  }

  def getUploadTimeStamp(userId: String)(implicit hc: HeaderCarrier) = {
    fetchFileSession(userId).map{
      case Some(fileSession) =>
        fileSession.uploadTimeStamp match {
          case Some(timeStamp) => timeStamp
          case _ => Logger.error("[SessionService][getUploadTimeStamp] no timestamp retrieved")
        }
      case _ => Logger.error("[SessionService][getUploadTimeStamp] no fileSession retrieved")
    }
  }



  def removeFileSessionFromCache(userId: String)(implicit hc: HeaderCarrier) = {
    shortLivedCache.remove(userId).map(_.status).recover {
    case ex: Throwable => Logger.error(s"unable to remove FileSession from cache  => " +
      s"${userId} , Exception is ${ex.getMessage}")
    //try again as the only option left if sessioncache fails
      shortLivedCache.remove(userId).map(_.status)
    }
  }
}

object ShortLivedCache extends ShortLivedCache {
  override val shortLivedCache: ShortLivedHttpCaching = RasShortLivedHttpCaching
  override val hoursToWaitForReUpload = ApplicationConfig.hoursToWaitForReUpload
}


