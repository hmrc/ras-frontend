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

package views

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import utils.RasTestHelper

class CannotUploadAnotherFileViewSpec extends AnyWordSpec with RasTestHelper {

  "cannot upload another file page" must {
    "contains the right title" in {
      val result = cannotUploadAnotherFileView()(fakeRequest, testMessages, mockAppConfig)
      doc(result).title() shouldBe Messages("cannot.upload.another.file.page.title")
    }

    "contains the right header" in {
      val result = cannotUploadAnotherFileView()(fakeRequest, testMessages, mockAppConfig)
      doc(result).getElementById("page-header").text shouldBe Messages("cannot.upload.another.file.page.header")
    }

    "contains a clarification paragraph" in {
      val result = cannotUploadAnotherFileView()(fakeRequest, testMessages, mockAppConfig)
      doc(result).getElementById("page-clarification").text shouldBe Messages(
        "cannot.upload.another.file.page.clarification"
      )
    }

    "contains a 'choose something else to do' button" in {
      val result = cannotUploadAnotherFileView()(fakeRequest, testMessages, mockAppConfig)
      doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
    }

    "contains the correct ga events" in {
      val result = cannotUploadAnotherFileView()(fakeRequest, testMessages, mockAppConfig)
      doc(result)
        .getElementById("choose-something-else-link")
        .attr("data-journey-click") shouldBe "Choose something else to do"
    }
  }

}
