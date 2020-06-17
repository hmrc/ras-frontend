/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import models.UserDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class UserDetailsConnectorSpec extends UnitSpec with RasTestHelper {

	def testConnector: UserDetailsConnector = new UserDetailsConnector(mockHttp)

  "Get User Details endpoint" must {

    "return whatever it receives" in {
      when(mockHttp.GET[UserDetails](any())(any(), any(), any())).
        thenReturn(Future.successful(UserDetails(None, None, "")))

      val response = Await.result(testConnector.getUserDetails("1234567890"), Duration.Inf)

      response shouldBe UserDetails(None, None, "")
    }

  }

}
