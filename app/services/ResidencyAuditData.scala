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

import models.{ConnectorFailure, ResidencyStatus}

object ResidencyAuditData {

  private def baseMap(userId: String, nino: String): Map[String, String] =
    Map("userIdentifier" -> userId, "requestSource" -> "FE_SINGLE", "nino" -> nino)

  def success(userId: String, nino: String, residencyStatus: ResidencyStatus): Map[String, String] = {
    val nextYearForecastResidencyStatus =
      residencyStatus.nextYearForecastResidencyStatus.map(nextCyStatus => "NextCYStatus" -> nextCyStatus)

    baseMap(userId, nino) ++
      Map(
        "successfulLookup" -> "true",
        "httpStatus"       -> "200",
        "CYStatus"         -> residencyStatus.currentYearResidencyStatus
      ) ++ nextYearForecastResidencyStatus
  }

  def failure(userId: String, nino: String, failure: ConnectorFailure): Map[String, String] = {
    val status      = failure.status.map(status => "httpStatus" -> status.toString)
    val parseErrors = failure.parseErrors.map(errors => "parseErrors" -> errors)

    baseMap(userId, nino) ++ Map("successfulLookup" -> "false", "reason" -> failure.reason.toString)
      ++ status ++ parseErrors
  }

}
