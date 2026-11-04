/*
 * Copyright 2026 HM Revenue & Customs
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

import models.{ConnectorFailure, FailureReason}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ResultsDownloadAuditDataSpec extends AnyWordSpec with Matchers {

  private val userId   = "Z123456"
  private val fileName = "testFile.csv"

  "success" should {
    "contain the user, request source, file name, outcome and a 200 status" in {
      ResultsDownloadAuditData.success(userId, fileName) shouldBe Map(
        "userIdentifier"     -> userId,
        "requestSource"      -> "FE_BULK",
        "fileName"           -> fileName,
        "successfulDownload" -> "true",
        "httpStatus"         -> "200"
      )
    }
  }

  "failure" should {

    "contain the reason and status when the failure has a status" in {
      val failure = ConnectorFailure(FailureReason.INTERNAL_SERVER_ERROR, status = Some(404))

      ResultsDownloadAuditData.failure(userId, fileName, failure) shouldBe Map(
        "userIdentifier"     -> userId,
        "requestSource"      -> "FE_BULK",
        "fileName"           -> fileName,
        "successfulDownload" -> "false",
        "reason"             -> "INTERNAL_SERVER_ERROR",
        "httpStatus"         -> "404"
      )
    }

    "leave out httpStatus when the failure has no status" in {
      // e.g. a timeout or connection failure – no response came back
      val failure = ConnectorFailure(FailureReason.TIMEOUT)

      ResultsDownloadAuditData.failure(userId, fileName, failure) shouldBe Map(
        "userIdentifier"     -> userId,
        "requestSource"      -> "FE_BULK",
        "fileName"           -> fileName,
        "successfulDownload" -> "false",
        "reason"             -> "TIMEOUT"
      )
    }
  }

}
