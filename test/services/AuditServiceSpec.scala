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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers.{contain, convertToAnyShouldWrapper}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import org.scalatest.WordSpecLike
import utils.RasTestHelper

trait AuditServiceSpec extends WordSpecLike with RasTestHelper with BeforeAndAfter {

	class TestService extends AuditService {
		override val connector: DefaultAuditConnector = mockAuditConnector
	}

  before {
    reset(mockAuditConnector)
  }

  "AuditService" must {

    val fakeAuditType = "fake-audit-type"
    val fakeEndpoint = "/fake-endpoint"

    val auditDataMap: Map[String, String] = Map("testKey" -> "testValue")

    "build an audit event with the correct mandatory details" in new TestService {

      audit(fakeAuditType, fakeEndpoint, auditDataMap)
      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event: DataEvent = captor.getValue

      event.auditSource shouldBe "ras-api"
      event.auditType shouldBe "fake-audit-type"
    }

    "build an audit event with the correct tags" in new TestService {

      audit(fakeAuditType, fakeEndpoint, auditDataMap)
      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event: DataEvent = captor.getValue

      event.tags should contain ("transactionName" -> "fake-audit-type")
      event.tags should contain("path" -> "/fake-endpoint")
      event.tags should contain key "clientIP"
    }

    "build an audit event with the correct detail" in new TestService {

      audit(fakeAuditType, fakeEndpoint, auditDataMap)
      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event: DataEvent = captor.getValue

      event.detail should contain ("testKey" -> "testValue")

      event.detail should contain key "Authorization"
    }

    "send an event via the audit connector" in new TestService {

      audit(fakeAuditType, fakeEndpoint, auditDataMap)
      verify(mockAuditConnector).sendEvent(any())(any(), any())
    }
  }
}
