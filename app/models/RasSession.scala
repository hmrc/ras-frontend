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

package models

import play.api.libs.json.{Json, OFormat}

case class RasSession(name:MemberName,
                      nino:MemberNino,
                      dateOfBirth:MemberDateOfBirth,
                      residencyStatusResult: Option[ResidencyStatusResult] = None,
                      uploadResponse: Option[UploadResponse] = None,
                      envelope: Option[Envelope] = None,
                      aFileIsInProcess: Option[Boolean] = None) {

  def selectKeysToCache[T](session: RasSession, key: CacheKey[T], value: Option[T]): RasSession = key match {
    case CacheKey.Name => handleNameCacheKey(session, value)
    case CacheKey.Nino => handleNinoCacheKey(session, value)
    case CacheKey.Dob => handleDobCacheKey(session, value)
    case CacheKey.StatusResult => handleStatusResultCacheKey(session, value)
    case CacheKey.UploadResponse => handleUploadResponseCacheKey(session, value)
    case CacheKey.Envelope => handleEnvelopeCacheKey(session, value)
    case CacheKey.All => RasSession.cleanSession
    case _ => throw new IllegalArgumentException("Mismatched key and value types")
  }

  private def handleNameCacheKey(session: RasSession, value: Option[_]): RasSession = value match {
    case Some(v: MemberName) => session.copy(name = v)
    case None => session.copy(name = RasSession.cleanMemberName)
    case _ => session
  }

  private def handleNinoCacheKey(session: RasSession, value: Option[_]): RasSession = value match {
    case Some(v: MemberNino) => session.copy(nino = v)
    case None => session.copy(nino = RasSession.cleanMemberNino)
    case _ => session
  }

  private def handleDobCacheKey(session: RasSession, value: Option[_]): RasSession = value match {
    case Some(v: MemberDateOfBirth) => session.copy(dateOfBirth = v)
    case None => session.copy(dateOfBirth = RasSession.cleanMemberDateOfBirth)
    case _ => session
  }

  private def handleStatusResultCacheKey(session: RasSession, value: Option[_]): RasSession = value match {
    case Some(v: ResidencyStatusResult) => session.copy(residencyStatusResult = Some(v))
    case None => session.copy(residencyStatusResult = None)
    case _ => session
  }

  private def handleUploadResponseCacheKey(session: RasSession, value: Option[_]): RasSession = value match {
    case Some(v: UploadResponse) => session.copy(uploadResponse = Some(v))
    case None => session.copy(uploadResponse = None)
    case _ => session
  }

  private def handleEnvelopeCacheKey(session: RasSession, value: Option[_]): RasSession = value match {
    case Some(v: Envelope) => session.copy(envelope = Some(v))
    case None => session.copy(envelope = None)
    case _ => session
  }
}

object RasSession{
  implicit val format: OFormat[RasSession] = Json.format[RasSession]

  val cleanMemberName: MemberName = MemberName("", "")
  val cleanMemberNino: MemberNino = MemberNino("")
  val cleanMemberDateOfBirth: MemberDateOfBirth = MemberDateOfBirth(RasDate(None, None, None))
  val cleanSession: RasSession = RasSession(cleanMemberName, cleanMemberNino, cleanMemberDateOfBirth)
}
