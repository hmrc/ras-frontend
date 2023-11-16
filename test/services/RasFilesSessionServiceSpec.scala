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

import models._
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{BeforeAndAfterEach, OptionValues, PrivateMethodTester}
import org.scalatestplus.play.PlaySpec
import repository.{FilesCacheRepositoryConfig, RasFilesSessionRepository}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import utils.RasTestHelper

import scala.concurrent.Future

class RasFilesSessionServiceSpec extends PlaySpec with RasTestHelper with OptionValues with PrivateMethodTester with BeforeAndAfterEach {

  val callbackData: CallbackData = CallbackData("1234", "file-id-1", "AVAILABLE", None)
  val resultsFile: ResultsFileMetaData = ResultsFileMetaData("file-id-1", Some("fileName.csv"), Some(1234L), 123, 1234L)
  val fileMetaData: FileMetadata = FileMetadata("file-id-1", Some("fileName.csv"), None)
  val newFileSession: FileSession = FileSession(None, None, "A123456", Some(DateTime.now().getMillis), None)
  val fileSession: FileSession = FileSession(Some(callbackData), Some(resultsFile), "A123456", Some(DateTime.now().getMillis), None)
  val failedFileSession: FileSession = FileSession(Some(callbackData.copy(status = "ERROR")), Some(resultsFile), "A123456", Some(DateTime.now().getMillis), None)

  val filesCacheRepositoryConfig = new FilesCacheRepositoryConfig(applicationConfig = applicationConfig)
  val filesSessionRepository = new RasFilesSessionRepository(filesCacheRepositoryConfig, timestampSupport = new CurrentTimestampSupport())

  val filesSessionService: RasFilesSessionService = new RasFilesSessionService(filesSessionRepository, applicationConfig)

  def updateFileSession(userId: String, fileSession: FileSession): Future[CacheItem] =
    filesSessionRepository.put[FileSession](userId)(DataKey("fileSession"), fileSession)
  class Setup(initializeCache: Boolean = true) {
    await {
      if (initializeCache) {
        for {
          _ <- filesSessionService.createFileSession("A123456", "envelopeId-1234")
        } yield ()
      } else {
        filesSessionRepository.deleteEntity("A123456")
      }
    }
  }

  override def beforeEach(): Unit =
    filesSessionRepository.collection.drop()

  "createFileSession" must {
    "return true if session created successfully" in new Setup(false) {
      val result: Boolean = filesSessionService.createFileSession("A123456", "envelopeId-1234").futureValue

      result shouldBe true
    }
  }

  "fetchFileSession" must {
    "return fileSession" in new Setup(true) {
      val result: FileSession = filesSessionService.fetchFileSession("A123456").futureValue.value

     result.copy(uploadTimeStamp = None) shouldBe newFileSession.copy(uploadTimeStamp = None)
    }

    "return None if file session was not found" in new Setup(false) {
      val result: Option[FileSession] = filesSessionService.fetchFileSession("A123456").futureValue

      result shouldBe None
    }
  }

  "removeFileSessionFromCache" must {
    "remove the file session from cache" in new Setup(true) {
      val result: Option[FileSession] = (for {
        _ <- filesSessionService.removeFileSessionFromCache("A123456")
        fileSession <- filesSessionService.fetchFileSession("A123456")
      } yield fileSession).futureValue

      result shouldBe None
    }
  }

  "hasBeen24HoursSinceTheUpload" must {
    "return true if more than 24 hours have passed since the upload" in {
      val hasBeen24HoursSinceTheUpload = PrivateMethod[Boolean](Symbol("hasBeen24HoursSinceTheUpload"))

      val fileUploadTime = System.currentTimeMillis() - 25 * 60 * 60 * 1000 // 25 hours ago

      val result = filesSessionService.invokePrivate(hasBeen24HoursSinceTheUpload(fileUploadTime))

      result shouldBe true
    }

    "return false if less than 24 hours have passed since the upload" in {
      val hasBeen24HoursSinceTheUpload = PrivateMethod[Boolean](Symbol("hasBeen24HoursSinceTheUpload"))

      val fileUploadTime = System.currentTimeMillis() - 23 * 60 * 60 * 1000 // 23 hours ago

      val result = filesSessionService.invokePrivate(hasBeen24HoursSinceTheUpload(fileUploadTime))

      result shouldBe false
    }
  }

  "failedProcessingUploadedFile" must {
    "return true if processing of uploaded file failed" in new Setup(true) {
      val result: Boolean = (for {
        _ <- updateFileSession("A123456", failedFileSession)
        failed <- filesSessionService.failedProcessingUploadedFile("A123456")
      } yield failed).futureValue

        result shouldBe true
    }

    "return false is processing of uploaded file was successful" in new Setup(true) {
      val result: Boolean = (for {
        _ <- updateFileSession("A123456", fileSession)
        failed <- filesSessionService.failedProcessingUploadedFile("A123456")
      } yield failed).futureValue

      result shouldBe false
    }

    "return true when the file session exists but results file is empty" in new Setup(true) {
      val fileSessionWithoutResultsFile: FileSession = FileSession(Some(callbackData), None, "A123456", Some(DateTime.now().minusDays(2).getMillis), None)

      val result: Boolean = (for {
        _ <- updateFileSession("A123456", fileSessionWithoutResultsFile)
        failed <- filesSessionService.failedProcessingUploadedFile("A123456")
      } yield failed).futureValue

      result shouldBe true
    }

    "return false when no upload timestamp is found" in new Setup(true) {
      val fileSessionWithoutTimestamp: FileSession = FileSession(Some(callbackData), Some(resultsFile), "A123456", None, None)

      val result: Boolean = (for {
        _ <- updateFileSession("A123456", fileSessionWithoutTimestamp)
        failed <- filesSessionService.failedProcessingUploadedFile("A123456")
      } yield failed).futureValue

      result shouldBe false
    }

    "return false when no file session is found" in new Setup(false) {
      val result: Boolean = filesSessionService.failedProcessingUploadedFile("A123456").futureValue

      result shouldBe false
    }
  }

  "errorInFileUpload" must {
    "return true if there was error during upload" in {
      val result: Boolean = filesSessionService.errorInFileUpload(failedFileSession)

      result shouldBe true
    }

    "remove file session from cache if there was error during upload" in {
      val result = (for {
        _ <- Future.successful(filesSessionService.errorInFileUpload(failedFileSession))
        fileSession <- filesSessionService.fetchFileSession(failedFileSession.userId)
      } yield fileSession).futureValue

      result shouldBe None
    }

    "return false if there was no error during upload" in {
      val result: Boolean = filesSessionService.errorInFileUpload(fileSession)

      result shouldBe false
    }

    "return false if userFile is not defined" in {
      val result: Boolean = filesSessionService.errorInFileUpload(fileSession.copy(userFile = None))

      result shouldBe false
    }
  }

  "getDownloadFileName" must {
    "return file name" in {
      val result: String = filesSessionService.getDownloadFileName(fileSession.copy(fileMetadata = Some(fileMetaData)))

      result shouldBe "fileName"
    }

    "return default file name" in {
      val result: String = filesSessionService.getDownloadFileName(fileSession)

      result shouldBe "Residency-status"
    }
  }

  "isFileInProgress" must {
    "return true if file is in progress" in new Setup(true) {
      updateFileSession("A123456", fileSession)

      val result: Boolean = filesSessionService.isFileInProgress("A123456").futureValue

      result shouldBe true
    }

    "return false if file session is not defined" in new Setup(true) {
      val result: Boolean = filesSessionService.isFileInProgress("B123456").futureValue

      result shouldBe false
    }
  }

  "determineFileStatus" must {
    "return Ready if results file available" in new Setup(true) {
      val result: FileUploadStatus.Value = (for {
        _ <- updateFileSession("A123456", fileSession.copy(resultsFile = Some(resultsFile)))
        status <- filesSessionService.determineFileStatus("A123456")
      } yield status).futureValue

      result shouldBe FileUploadStatus.Ready
    }

    "return InProgress if file processing is not completed" in new Setup(true) {
      val result: FileUploadStatus.Value = filesSessionService.determineFileStatus("A123456").futureValue

      result shouldBe FileUploadStatus.InProgress
    }

    "return UploadError if failedProcessingUploadedFile returns true" in new Setup(false) {
      val result: FileUploadStatus.Value = (for {
        _ <- updateFileSession("A123456", failedFileSession.copy(resultsFile = None))
        status <- filesSessionService.determineFileStatus("A123456")
      } yield status).futureValue

      result shouldBe FileUploadStatus.UploadError
    }

    "return NoFileSession if no entry in db for userId" in new Setup(false) {
      val result: FileUploadStatus.Value = filesSessionService.determineFileStatus("A123456").futureValue

      result shouldBe FileUploadStatus.NoFileSession
    }

    "return TimeExpiryError if no entry in db for userId " in new Setup(true) {
      val result: FileUploadStatus.Value = (for {
        _ <- updateFileSession("A123456", failedFileSession.copy(resultsFile = None, uploadTimeStamp = Some(DateTime.now().minusDays(2).getMillis)))
        status <- filesSessionService.determineFileStatus("A123456")
      } yield status).futureValue

      result shouldBe FileUploadStatus.TimeExpiryError
    }
  }
}
