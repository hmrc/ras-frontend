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

import config.ApplicationConfig
import connectors.FilesSessionConnector
import models.FileUploadStatus.{InProgress, NoFileSession, Ready, TimeExpiryError, UploadError}
import models.{CreateFileSessionRequest, FileSession, FileUploadStatus}
import org.joda.time.DateTime
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FilesSessionService @Inject()(fileSessionConnector: FilesSessionConnector,
                                    appConfig: ApplicationConfig) extends Logging {

  lazy val hoursToWaitForReUpload: Int = appConfig.hoursToWaitForReUpload
  private val STATUS_AVAILABLE: String = "AVAILABLE"
  private val defaultDownloadName: String = "Residency-status"

  def createFileSession(userId: String, envelopeId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    fileSessionConnector.createFileSession(CreateFileSessionRequest(userId,envelopeId)).recover {
      case ex: Throwable =>
        logger.error(s"[FilesSessionService][createFileSession] Unable to create FileSession to cache => " +
          s"$userId, envelopeId: $envelopeId, Exception is ${ex.getMessage}")
        false
    }
  }

  def fetchFileSession(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FileSession]] = {
    fileSessionConnector.fetchFileSession(userId).recover {
      case ex: Throwable => logger.error(s"[FilesSessionService][fetchFileSession] Unable to fetch FileSession from cache => " +
        s"$userId , Exception is ${ex.getMessage}")
        None
    }
  }

  private def hasBeen24HoursSinceTheUpload(fileUploadTime: Long): Boolean = {
    new DateTime(fileUploadTime).plusHours(hoursToWaitForReUpload).isBefore(DateTime.now.getMillis)
  }

  def failedProcessingUploadedFile(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    fetchFileSession(userId).map {
      case Some(fileSession) =>
        fileSession.uploadTimeStamp match {
          case Some(timestamp) =>
            errorInFileUpload(fileSession) || (hasBeen24HoursSinceTheUpload(timestamp) && fileSession.resultsFile.isEmpty)
          case _ =>
            logger.error("[FilesSessionService][failedProcessingUploadedFile] No upload timestamp found")
            false
        }
      case _ =>
        logger.error("[FilesSessionService][failedProcessingUploadedFile] No file session found")
        false
    }
  }

  def errorInFileUpload(fileSession: FileSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Boolean = {
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
    if (name.indexOf(".") > 0) {
      name.take(name.lastIndexOf("."))
    } else {
      name
    }
  }

  def isFileInProgress(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    fetchFileSession(userId).map {
      case Some(fileSession) =>
        fileSession.resultsFile.isDefined || fileSession.uploadTimeStamp.exists(!hasBeen24HoursSinceTheUpload(_))
      case None =>
        logger.warn(s"[FilesSessionService][isFileInProgress] FileSession not defined for $userId")
        false
    }.recover {
      case ex: Throwable =>
        logger.error(s"[FilesSessionService][isFileInProgress] Unable to fetch FileSession from cache to check " +
          s"isFileInProgress => $userId , Exception is ${ex.getMessage}")
        false
    }
  }

  def determineFileStatus(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FileUploadStatus.Value] = {
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

  def removeFileSessionFromCache(userId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    fileSessionConnector.deleteFileSession(userId)
      .recover {
      case ex: Throwable =>
        logger.error(s"[FilesSessionService][removeFileSessionFromCache] Unable to remove FileSession from cache  => " +
        s"$userId , Exception is ${ex.getMessage}")
        false
    }
  }
}
