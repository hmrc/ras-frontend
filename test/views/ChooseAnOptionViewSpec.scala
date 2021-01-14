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

import models.FileUploadStatus._
import org.joda.time.DateTime
import play.api.i18n.Messages
import uk.gov.hmrc.play.test.UnitSpec
import utils.RasTestHelper


class ChooseAnOptionViewSpec extends UnitSpec with RasTestHelper {

	val mockExpiryTimeStamp: Long = new DateTime().getMillis

	def formattedExpiryDate(timestamp: Long): Option[String] = {
		val expiryDate = new DateTime(timestamp)
		Some(s"${expiryDate.toString("H:mma").toLowerCase()} on ${expiryDate.toString("EEEE d MMMM yyyy")}")
	}

	private def formattedUploadDate(timestamp: Long): Option[String] = {
		val uploadDate = new DateTime(timestamp)

		val todayOrYesterday = if (uploadDate.toLocalDate.isEqual(DateTime.now.toLocalDate)) {
			"today"
		} else {
			"yesterday"
		}
		Some(s"$todayOrYesterday at ${uploadDate.toString("h:mma").toLowerCase()}")
	}

	"choose an option page" when {
		"the file status is NoFileSession" should {
			"contain an Upload a file link" in {
				val result = views.html.choose_an_option(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("upload-link").text() shouldBe Messages("upload.file")
				doc(result).getElementById("upload-link").attr("href") should include("/upload-a-file")
				doc(result).getElementById("upload-link").attr("data-journey-click") shouldBe "link - click:Choose option to get residency status:Upload a file"
			}

			"contain the correct title and header" in {
				val result = views.html.choose_an_option(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
				doc(result).title shouldBe Messages("chooseAnOption.page.title", Messages("filestatus.NoFileSession"))
				doc(result).getElementsByClass("heading-xlarge").text shouldBe Messages("chooseAnOption.page.header")
			}

			"contain the single member h2" in {
				val result = views.html.choose_an_option(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("task-list-section").get(0).html() shouldBe Messages("single.member.subheading")
			}

			"contain the enter a members detail link" in {
				val result = views.html.choose_an_option(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementById("single-member-link").text shouldBe Messages("enter.members.details")
				doc(result).getElementById("single-member-link").attr("href") should include("/member-name")
				doc(result).getElementById("single-member-link").attr("data-journey-click") shouldBe "link - click:Choose option to get residency status:Enter a members details"
			}

			"contain the Multiple members h2" in {
				val result = views.html.choose_an_option(NoFileSession, None)(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("task-list-section").get(1).html() shouldBe Messages("multiple.members.subheading")
			}
		}

		"the file status is Ready" should {
			"contain a download your results link" in {
				val result = views.html.choose_an_option(Ready, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("task-name").get(1).html() shouldBe Messages("download.results")
				doc(result).getElementById("download-result-link").attr("href") should include("/residency-status-added")
				doc(result).getElementById("download-result-link").attr("data-journey-click") shouldBe "link - click:Choose option to get residency status:Download your results"

			}

			"contain a File ready Icon" in {
				val result = views.html.choose_an_option(Ready, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("task-completed").text shouldBe "FILE READY"
			}

			"contain File ready paragraph" in {
				val date = new DateTime(mockExpiryTimeStamp)
				val result = views.html.choose_an_option(Ready, formattedExpiryDate(mockExpiryTimeStamp))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("paragraph-info").text shouldBe Messages("result.timescale", s"${date.toString("H:mma").toLowerCase()} on ${date.toString("EEEE d MMMM yyyy")}")
			}
		}

		"for Processing Only" should {
			"contain a Processing icon" in {
				val result = views.html.choose_an_option(InProgress, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("task-completed").text shouldBe Messages("file.processing")

			}

			"contain File processing paragraphs with todays date" in {
				val date = DateTime.now().minusDays(1)
				val result = views.html.choose_an_option(InProgress, formattedUploadDate(mockExpiryTimeStamp))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("paragraph-info").get(0).text() shouldBe Messages("file.processing") + Messages("file.upload.time",
					s"${Messages("today")} at ${date.toString("h:mma").toLowerCase()}")
				doc(result).getElementsByClass("paragraph-info").get(1).text() shouldBe Messages("file.size.info")
				doc(result).getElementsByClass("paragraph-info").get(2).text() shouldBe Messages("processing.file")

			}

			"contain File processing paragraphs with yesterday date" in {
				val date = DateTime.now().minusDays(1)
				val result = views.html.choose_an_option(InProgress, formattedUploadDate(date.getMillis))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("paragraph-info").get(0).text() shouldBe Messages("file.processing") + Messages("file.upload.time",
					s"${Messages("yesterday")} at ${date.toString("h:mma").toLowerCase()}")
				doc(result).getElementsByClass("paragraph-info").get(1).text() shouldBe Messages("file.size.info")
				doc(result).getElementsByClass("paragraph-info").get(2).text() shouldBe Messages("processing.file")
			}
		}

		"for UploadError Only" should {
			"contain an upload your file again link" in {
				val result = views.html.choose_an_option(UploadError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("file-problem-link").text() shouldBe Messages("upload.file.again")
				doc(result).getElementsByClass("file-problem-link").attr("href") should include("/upload-a-file")
				doc(result).getElementsByClass("file-problem-link").attr("data-journey-click") shouldBe "link - click:Choose option to get residency status:Upload a file"
			}

			"contain a File problem icon" in {
				val result = views.html.choose_an_option(UploadError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("task-completed").text shouldBe "FILE PROBLEM"
			}

			"contain File problem paragraphs" in {
				val result = views.html.choose_an_option(UploadError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("paragraph-info").text shouldBe Messages("file.problem.paragraph", Messages("upload.file.again"))
			}
		}

		"for TimeExpiryError Only" should {
			"contain an upload your file again link" in {
				val result = views.html.choose_an_option(TimeExpiryError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("file-problem-link").text() shouldBe Messages("upload.file.again")
				doc(result).getElementsByClass("file-problem-link").attr("href") should include("/upload-a-file")
				doc(result).getElementsByClass("file-problem-link").attr("data-journey-click") shouldBe "link - click:Choose option to get residency status:Upload a file"
			}

			"contain a File problem icon" in {
				val result = views.html.choose_an_option(TimeExpiryError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("task-completed").text shouldBe "FILE PROBLEM"

			}

			"contain File problem paragraphs" in {
				val result = views.html.choose_an_option(TimeExpiryError, Some("None"))(fakeRequest, testMessages, mockAppConfig)
				doc(result).getElementsByClass("paragraph-info").text shouldBe Messages("file.problem.paragraph", Messages("upload.file.again"))
			}
		}
	}
}
