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
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet }

class UserDetailsConnectorSpec extends PlaySpec
  with MockitoSugar
  with GuiceOneAppPerSuite {

  implicit val hc = HeaderCarrier()

  "Get User Details endpoint" must {

    "return whatever it receives" in {
      when(mockHttpGet.GET[UserDetails](any())(any(), any(), any())).
        thenReturn(Future.successful(UserDetails(None, None, "")))

      val response = Await.result(SUT.getUserDetails("1234567890"), Duration.Inf)

      response mustBe UserDetails(None, None, "")
    }

  }

  val mockHttpGet = mock[HttpGet]

  object SUT extends UserDetailsConnector {
    override val httpGet = mockHttpGet
  }

}
