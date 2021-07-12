/*
 * Copyright 2021 HM Revenue & Customs
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

import org.joda.time.DateTime
import org.scalatest.Matchers.{convertToAnyShouldWrapper, include}
import play.api.i18n.Messages
import views.helpers.ViewSpecHelper


class UploadResultViewSpec extends ViewSpecHelper {

	private def isBeforeApr6(timestamp: Long) : Boolean = {
		val uploadDate = new DateTime(timestamp)
		uploadDate.isBefore(DateTime.parse(s"${uploadDate.getYear}-04-06"))
	}

	def formattedExpiryDate(timestamp: Long): String = {
		val expiryDate = new DateTime(timestamp).plusDays(3)
		s"${expiryDate.toString("H:mma").toLowerCase()} on ${expiryDate.toString("EEEE d MMMM yyyy")}"
	}

	val now: Long = DateTime.now().getMillis

	"upload result page" must {
		behave like pageWithFeedbackLink(
			uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig))

		"contain the correct page title" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).title shouldBe Messages("upload.result.page.title")
		}

		"contain a back link pointing to" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("href") should include("/")
		}

		"contain the correct page header" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementsByClass("govuk-heading-xl").text shouldBe Messages("upload.result.page.header")
		}

		"contain a icon file image" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("attachment-1").children().first().attr("src") should include("icon-file-download.png")
		}

		"contain a result link with the correct file name" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("result-link").text shouldBe Messages("residency.status.result", "filename")
		}

		"contain a result link pointing to the results file" in {
			val result = uploadResultView("testFileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("result-link").attr("href") should include(s"/results/testFileId")
		}

		"contain expiry date message" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("expiry-date-message").text shouldBe Messages("expiry.date.message", formattedExpiryDate(now))
		}

		"contain a what to do next header" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("whatnext-header").text shouldBe Messages("match.found.what.happens.next")
		}

		"contain what to do next content" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("whatnext-content").text shouldBe Messages("upload.result.what.next", Messages("upload.result.member.contact"))
		}

		"contain an contact HMRC link" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("contact-hmrc-link").text shouldBe Messages("upload.result.member.contact")
		}

		"contains an HMRC link that opens help page in new tab" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("contact-hmrc-link").attr("href") shouldBe "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/national-insurance-numbers"
			doc(result).getElementById("contact-hmrc-link").attr("target") shouldBe "_blank"
		}

		"contain a deletion message" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("deletion-message").text shouldBe Messages("deletion.message")
		}

		"contain the correct ga events when upload date is 01/01/2018 (CY+1)" in {
			val mockUploadTimeStamp = DateTime.parse("2018-01-01").getMillis
			val result = uploadResultView("fileId", formattedExpiryDate(mockUploadTimeStamp), isBeforeApr6(mockUploadTimeStamp), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:Residency status upload added CY & CY + 1:Back"
			doc(result).getElementById("result-link").attr("data-journey-click") shouldBe "link - click:Residency status upload added CY & CY + 1:ResidencyStatusResults CY & CY + 1 CSV"
			doc(result).getElementById("choose-something-else-link").attr("data-journey-click") shouldBe "Choose something else to do"
			doc(result).getElementById("contact-hmrc-link").attr("data-journey-click") shouldBe "link - click:Residency status upload added CY & CY + 1:Member must contact HMRC"
		}

		"contain a cy message when upload date is 06/04/2018" in {
			val mockUploadTimeStamp = DateTime.parse("2018-04-06").getMillis
			val result = uploadResultView("fileId", formattedExpiryDate(mockUploadTimeStamp), isBeforeApr6(mockUploadTimeStamp), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("cy-message").text shouldBe Messages("cy.message", (1000 + 1).toString, (1000 + 2).toString)
		}

		"contain the correct ga events when upload date is 06/04/2018 (CY only)" in {
			val mockUploadTimeStamp = DateTime.parse("2018-04-06").getMillis
			val result = uploadResultView("fileId", formattedExpiryDate(mockUploadTimeStamp), isBeforeApr6(mockUploadTimeStamp), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("back").attr("data-journey-click") shouldBe "navigation - link:Residency status upload added CY:Back"
			doc(result).getElementById("result-link").attr("data-journey-click") shouldBe "link - click:Residency status upload added CY:ResidencyStatusResults CY CSV"
			doc(result).getElementById("choose-something-else-link").attr("data-journey-click") shouldBe "Choose something else to do"
			doc(result).getElementById("contact-hmrc-link").attr("data-journey-click") shouldBe "link - click:Residency status upload added CY:Member must contact HMRC"

		}

		"contain a cy message when upload date is 31/12/2018" in {
			val mockUploadTimeStamp = DateTime.parse("2018-12-31").getMillis
			val result = uploadResultView("fileId", formattedExpiryDate(mockUploadTimeStamp), isBeforeApr6(mockUploadTimeStamp), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("cy-message").text shouldBe Messages("cy.message", (1000 + 1).toString, (1000 + 2).toString)
		}

		"contain a button to choose something else to do" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else").text shouldBe Messages("choose.something.else")
		}

		"contain a button to choose something else to do which points to choose an option page" in {
			val result = uploadResultView("fileId", formattedExpiryDate(now), isBeforeApr6(now), currentTaxYear = 1000, "filename")(fakeRequest, testMessages, mockAppConfig)
			doc(result).getElementById("choose-something-else-link").attr("href") should include("/")
		}
	}
}
