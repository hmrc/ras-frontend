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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.cache.client.ShortLivedHttpCaching

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SessionService extends SessionService


trait SessionService extends SessionCacheWiring {

  val RAS_SESSION_KEY = "ras_session"
  val cleanSession = RasSession(
    "",
    MemberName("", ""),
    MemberNino(""),
    MemberDateOfBirth(RasDate(None, None, None)),
    ResidencyStatusResult("", None, "", "", "", "", ""),
    None)

  def fetchRasSession()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {
    sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) map (rasSession => rasSession)
  }

  def resetRasSession()(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {
    sessionCache.cache[RasSession](RAS_SESSION_KEY, cleanSession) map (cacheMap => Some(cleanSession))
  }

  def cacheWhatDoYouWantToDo(userChoice: String)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

    val result = sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY) flatMap { currentSession =>
      sessionCache.cache[RasSession](RAS_SESSION_KEY,
        currentSession match {
          case Some(returnedSession) => returnedSession.copy(userChoice = userChoice)
          case None => cleanSession.copy(userChoice = userChoice)
        }
      )
    }

    result.map(cacheMap => {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    })
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

  val STATUS_AVAILABLE: String = "AVAILABLE"

  val defaultDownloadName = "Residency-status"

  def createFileSession(userId: String, envelopeId: String)(implicit hc: HeaderCarrier):Future[Boolean] = {

    shortLivedCache.cache[FileSession](source, userId, "fileSession",
      FileSession(None, None, userId, Some(DateTime.now().getMillis), None)).map(res => true) recover {
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

  def hasBeen24HoursSinceTheUpload(fileUploadTime: Long) = {
    new DateTime(fileUploadTime).plusHours(hoursToWaitForReUpload).isBefore(DateTime.now.getMillis)
  }

  def failedProcessingUploadedFile(userId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    fetchFileSession(userId).map {
      case Some(fileSession) =>
        fileSession.uploadTimeStamp match {
          case Some(timestamp) =>
            errorInFileUpload(fileSession) || (hasBeen24HoursSinceTheUpload(timestamp) && !fileSession.resultsFile.isDefined)
          case _ =>
            Logger.error("[ShortLivedCache][failedProcessingUploadedFile] no upload timestamp found")
            false
        }
      case _ =>
        Logger.error("[ShortLivedCache][failedProcessingUploadedFile] no file session found")
        false
    }
  }

  def errorInFileUpload(fileSession: FileSession)(implicit hc: HeaderCarrier): Boolean = {
    fileSession.userFile match {
      case Some(userFile) =>
        userFile.status match {
          case STATUS_AVAILABLE => false
          case _ => {
            removeFileSessionFromCache(fileSession.userId)
            true
          }
        }
      case _ => false
    }
  }

  def getDownloadFileName(fileSession: FileSession)(implicit hc: HeaderCarrier): String = {
    val name = fileSession.fileMetadata.flatMap(_.name).getOrElse(defaultDownloadName)
    if (name.indexOf(".") > 0)
      name.take(name.lastIndexOf("."))
    else
      name
  }

  def isFileInProgress(userId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    fetchFileSession(userId).map(fileSession =>
      fileSession.isDefined match {
        case true =>
          fileSession.get.resultsFile.isDefined || !hasBeen24HoursSinceTheUpload(fileSession.get.uploadTimeStamp.get)
        case false =>
          Logger.error("fileSession not defined for " + userId)
          false
    }).recover {
      case ex: Throwable =>
        Logger.error(s"unable to fetch FileSession from cache to check isFileInProgress => ${userId} , Exception is ${ex.getMessage}")
        false
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


