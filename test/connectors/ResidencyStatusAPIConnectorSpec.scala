/*
 * Copyright 2018 HM Revenue & Customs
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

import models._
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet }

class ResidencyStatusAPIConnectorSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ServicesConfig {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestConnector extends ResidencyStatusAPIConnector {
    override val http: HttpGet = mock[HttpGet]
  }

  "Residency Status API connector" should {

    "send a get request to residency status service" in {

      val uuid = "633e0ee7-315b-49e6-baed-d79c3dffe467"

      val expectedResponse = ResidencyStatus("scotResident","otherUKResident")

      when(TestConnector.http.GET[ResidencyStatus](any())(any(),any(), any())).thenReturn(Future.successful(expectedResponse))

      val result = TestConnector.getResidencyStatus(uuid)

      await(result) mustBe expectedResponse

    }


  }

}
