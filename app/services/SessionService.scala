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

import config._
import models.FileUploadStatus._
import models._
import org.joda.time.DateTime
import play.api.Logging
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionService @Inject()(val http: DefaultHttpClient,
                               val sessionCache: RasSessionCache
                              )(implicit ec: ExecutionContext) {

  val RAS_SESSION_KEY: String = "ras_session"

  def fetchRasSession()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = {
    sessionCache.fetchAndGetEntry[RasSession](RAS_SESSION_KEY)
  }

  def cacheName(value: MemberName)(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Name, Some(value))
  def cacheNino(value: MemberNino)(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Nino, Some(value))
  def cacheDob(value: MemberDateOfBirth)(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Dob, Some(value))
  def cacheUploadResponse(value: UploadResponse)(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.UploadResponse, Some(value))
  def cacheEnvelope(value: Envelope)(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Envelope, Some(value))
  def cacheResidencyStatusResult(value: ResidencyStatusResult)(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.StatusResult, Some(value))

  def resetCacheName()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Name)
  def resetCacheNino()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Nino)
  def resetCacheDob()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Dob)
  def resetCacheUploadResponse()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.UploadResponse)
  def resetCacheEnvelope()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.Envelope)
  def resetCacheResidencyStatusResult()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.StatusResult)

  def resetRasSession()(implicit hc: HeaderCarrier): Future[Option[RasSession]] = cache(CacheKey.All)

  private def cache[T](key: CacheKey[T], value: Option[T] = None)(implicit hc: HeaderCarrier): Future[Option[RasSession]] = {
    for {
      currentSession <- fetchRasSession()
      session = currentSession.getOrElse(RasSession.cleanSession)
      cacheMap <- sessionCache.cache[RasSession](RAS_SESSION_KEY, session.selectKeysToCache(session, key, value))
    } yield {
      cacheMap.getEntry[RasSession](RAS_SESSION_KEY)
    }
  }
}


class ShortLivedCache @Inject()(val shortLiveCache : RasShortLivedHttpCaching,
                                appConfig: ApplicationConfig,
                                applicationCrypto: ApplicationCrypto
                               )(implicit ec: ExecutionContext) extends Logging {

  private val source = "ras"
  lazy val hoursToWaitForReUpload: Int = appConfig.hoursToWaitForReUpload

  val STATUS_AVAILABLE: String = "AVAILABLE"

  val defaultDownloadName = "Residency-status"

  def createFileSession(userId: String, envelopeId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    shortLiveCache.cache[FileSession](source, userId, "fileSession",
      FileSession(None, None, userId, Some(DateTime.now().getMillis), None)).map(_ => true) recover {
      case ex: Throwable => logger.error(s"unable to create FileSession to cache => " +
        s"$userId , envelopeId :$envelopeId,  Exception is ${ex.getMessage}")
        false
      //retry or what is the other option?
    }
  }

  def fetchFileSession(userId: String)(implicit hc: HeaderCarrier): Future[Option[FileSession]] = {
    shortLiveCache.fetchAndGetEntry[FileSession](source, userId, "fileSession").recover {
      case ex: Throwable => logger.error(s"unable to fetch FileSession from cache => " +
        s"$userId , Exception is ${ex.getMessage}")
        None
    }
  }

  def hasBeen24HoursSinceTheUpload(fileUploadTime: Long): Boolean = {
    new DateTime(fileUploadTime).plusHours(hoursToWaitForReUpload).isBefore(DateTime.now.getMillis)
  }

  def failedProcessingUploadedFile(userId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    fetchFileSession(userId).map {
      case Some(fileSession) =>
        fileSession.uploadTimeStamp match {
          case Some(timestamp) =>
            errorInFileUpload(fileSession) || (hasBeen24HoursSinceTheUpload(timestamp) && fileSession.resultsFile.isEmpty)
          case _ =>
            logger.error("[ShortLivedCache][failedProcessingUploadedFile] no upload timestamp found")
            false
        }
      case _ =>
        logger.error("[ShortLivedCache][failedProcessingUploadedFile] no file session found")
        false
    }
  }

  def errorInFileUpload(fileSession: FileSession)(implicit hc: HeaderCarrier): Boolean = {
    fileSession.userFile match {
      case Some(userFile) =>
        userFile.status match {
          case STATUS_AVAILABLE => false
          case _ =>
            removeFileSessionFromCache(fileSession.userId)
            true
        }
      case _ => false
    }
  }

  def getDownloadFileName(fileSession: FileSession): String = {
    val name = fileSession.fileMetadata.flatMap(_.name).getOrElse(defaultDownloadName)
    if (name.indexOf(".") > 0)
      name.take(name.lastIndexOf("."))
    else
      name
  }

  def isFileInProgress(userId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    fetchFileSession(userId).map(fileSession =>
      if (fileSession.isDefined) {
        fileSession.get.resultsFile.isDefined || !hasBeen24HoursSinceTheUpload(fileSession.get.uploadTimeStamp.get)
      } else {
        logger.warn(s"[ShortLivedCache][isFileInProgress] fileSession not defined for $userId")
        false
      }).recover {
      case ex: Throwable =>
        logger.error(s"[ShortLivedCache][isFileInProgress] unable to fetch FileSession from cache to check isFileInProgress => $userId , Exception is ${ex.getMessage}")
        false
    }
  }

  def determineFileStatus(userId: String)(implicit hc: HeaderCarrier): Future[FileUploadStatus.Value] = {
    fetchFileSession(userId).flatMap {
      case Some(fileSession) =>
        fileSession.resultsFile match {
          case Some(_) => Future.successful(Ready)
          case _ => isFileInProgress(userId).flatMap {
            case true =>
              failedProcessingUploadedFile(userId).flatMap {
                case true => Future.successful(UploadError)
                case _ => Future.successful(InProgress)
              }
            case _ => Future.successful(TimeExpiryError)
          }
        }
      case _ => Future.successful(NoFileSession)
    }
  }

  def removeFileSessionFromCache(userId: String)(implicit hc: HeaderCarrier): Future[Any] = {
    shortLiveCache.remove(userId).map(_.status).recover {
    case ex: Throwable => logger.error(s"[ShortLivedCache][removeFileSessionFromCache] unable to remove FileSession from cache  => " +
      s"$userId , Exception is ${ex.getMessage}")
    //try again as the only option left if sessioncache fails
      shortLiveCache.remove(userId).map(_.status)
    }
  }
}
