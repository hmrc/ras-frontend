/*
 * Copyright 2017 HM Revenue & Customs
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

import config.SessionCacheWiring
import models._
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SessionService extends SessionService


trait SessionService extends SessionCacheWiring {

  val RAS_SESSION_KEY = "ras_session"
  val cleanSession = RasSession(MemberName("",""),
                                MemberNino(""),
                                MemberDateOfBirth(RasDate(None,None,None)),
                                ResidencyStatusResult("","","","","","",""),
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

  def cacheUploadResponse (uploadResponse: UploadResponse)(implicit request: Request[_], hc: HeaderCarrier): Future[Option[RasSession]] = {

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

}


