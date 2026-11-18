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

import models.{ConnectorFailure, FailureReason, ResidencyStatus}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.GatewayTimeoutException

class ResidencyAuditDataSpec extends AnyWordSpec with Matchers {

  private val userId = "A123456"
  private val nino   = "AB123456C"

  "success" should {

    "contain the user, request source, nino, outcome, status and both residency statuses" in {
      ResidencyAuditData.success(userId, nino, ResidencyStatus("scotResident", Some("otherUKResident"))) shouldBe Map(
        "userIdentifier"   -> userId,
        "requestSource"    -> "FE_SINGLE",
        "nino"             -> nino,
        "successfulLookup" -> "true",
        "httpStatus"       -> "200",
        "CYStatus"         -> "scotResident",
        "NextCYStatus"     -> "otherUKResident"
      )
    }

    "leave out NextCYStatus when there is no next year forecast" in {
      ResidencyAuditData.success(userId, nino, ResidencyStatus("scotResident", None)) shouldBe Map(
        "userIdentifier"   -> userId,
        "requestSource"    -> "FE_SINGLE",
        "nino"             -> nino,
        "successfulLookup" -> "true",
        "httpStatus"       -> "200",
        "CYStatus"         -> "scotResident"
      )
    }
  }

  "failure" should {

    "contain the reason and status when the failure has a status" in {
      val failure = ConnectorFailure(FailureReason.MATCHING_FAILED, status = Some(403))

      ResidencyAuditData.failure(userId, nino, failure) shouldBe Map(
        "userIdentifier"   -> userId,
        "requestSource"    -> "FE_SINGLE",
        "nino"             -> nino,
        "successfulLookup" -> "false",
        "reason"           -> "MATCHING_FAILED",
        "httpStatus"       -> "403"
      )
    }

    "leave out httpStatus when the failure has no status" in {
      val failure = ConnectorFailure(FailureReason.TIMEOUT)

      ResidencyAuditData.failure(userId, nino, failure) shouldBe Map(
        "userIdentifier"   -> userId,
        "requestSource"    -> "FE_SINGLE",
        "nino"             -> nino,
        "successfulLookup" -> "false",
        "reason"           -> "TIMEOUT"
      )
    }

    "include parseErrors when the failure has them" in {
      val failure = ConnectorFailure(
        FailureReason.INVALID_RESPONSE,
        status = Some(200),
        parseErrors = Some("/currentYearResidencyStatus: error.path.missing")
      )

      ResidencyAuditData.failure(userId, nino, failure) shouldBe Map(
        "userIdentifier"   -> userId,
        "requestSource"    -> "FE_SINGLE",
        "nino"             -> nino,
        "successfulLookup" -> "false",
        "reason"           -> "INVALID_RESPONSE",
        "httpStatus"       -> "200",
        "parseErrors"      -> "/currentYearResidencyStatus: error.path.missing"
      )
    }

    "not include anything from the cause" in {
      val failure = ConnectorFailure(
        FailureReason.TIMEOUT,
        cause = Some(new GatewayTimeoutException("POST of 'http://localhost/residency-status' timed out"))
      )

      ResidencyAuditData.failure(userId, nino, failure) shouldBe Map(
        "userIdentifier"   -> userId,
        "requestSource"    -> "FE_SINGLE",
        "nino"             -> nino,
        "successfulLookup" -> "false",
        "reason"           -> "TIMEOUT"
      )
    }
  }

}
