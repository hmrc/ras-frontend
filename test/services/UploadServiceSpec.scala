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

import connectors.{FileUploadConnector, FileUploadFrontendConnector}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class UploadServiceSpec extends UnitSpec with OneServerPerSuite with ScalaFutures with MockitoSugar {

  val mockFileUploadConnector = mock[FileUploadConnector]
  val mockFileUploadFrontendConnector = mock[FileUploadFrontendConnector]

  object TestUploadService extends UploadService {
    override val fileUploadConnector = mockFileUploadConnector
    override val fileUploadFrontendConnector = mockFileUploadFrontendConnector
  }

  "File upload service" when {

    "calling uploadFile" should {

      "return true if a file has been uploaded successfully" in {
        val fileUploadConnectorResponse = HttpResponse(201,None,Map("Location" -> List("localhost:8898/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653")),None)
        when(TestUploadService.fileUploadConnector.getEnvelope()(any())).thenReturn(Future.successful(fileUploadConnectorResponse))
        val result = await(TestUploadService.uploadFile)
        result shouldBe true
      }

      "return false if a file fails to upload" in {
        val fileUploadConnectorResponse = HttpResponse(400,None,Map(),None)
        when(TestUploadService.fileUploadConnector.getEnvelope()(any())).thenReturn(Future.successful(fileUploadConnectorResponse))
        val result = await(TestUploadService.uploadFile)
        result shouldBe false
      }
    }

    "calling obtainEnvelopeId" should {

      "extract envelope id from successful file upload response" in {
        val fileUploadConnectorResponse = HttpResponse(201,None,Map("Location" -> List("localhost:8898/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653")),None)
        when(TestUploadService.fileUploadConnector.getEnvelope()(any())).thenReturn(Future.successful(fileUploadConnectorResponse))
        val result = await(TestUploadService.obtainUploadEnvelopeId)
        result shouldBe Some("0b215e97-11d4-4006-91db-c067e74fc653")
      }

      "handle cases where file upload responds with success but envelope id cannot be extracted from the header" in {
        val fileUploadConnectorResponse = HttpResponse(201,None,Map("Location" -> List("localhost:8898/file-upload/envelopes/")),None)
        when(TestUploadService.fileUploadConnector.getEnvelope()(any())).thenReturn(Future.successful(fileUploadConnectorResponse))
        val result = await(TestUploadService.obtainUploadEnvelopeId)
        result shouldBe None
      }

      "handle cases where a bad request has been returned from file upload controller" in {
        val fileUploadConnectorResponse = HttpResponse(400,None,Map(),None)
        when(TestUploadService.fileUploadConnector.getEnvelope()(any())).thenReturn(Future.successful(fileUploadConnectorResponse))
        val result = await(TestUploadService.obtainUploadEnvelopeId)
        result shouldBe None
      }

    }


  }

}
