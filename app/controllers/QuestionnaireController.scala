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

package controllers

import config.ApplicationConfig
import javax.inject.Inject
import models.Questionnaire
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuditService
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}


class QuestionnaireController @Inject()(val connector: DefaultAuditConnector,
																				val mcc: MessagesControllerComponents,
																				implicit val appConfig: ApplicationConfig,
                                        feedbackQuestionaireView: views.html.feedback.feedbackQuestionaire,
                                        feedbackThankYouView: views.html.feedback.thanks
																			 ) extends FrontendController(mcc) with AuditService {
	implicit val ec: ExecutionContext = mcc.executionContext

  def showQuestionnaire: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(feedbackQuestionaireView(Questionnaire.form)))
  }

  def submitQuestionnaire: Action[AnyContent] = Action.async { implicit request =>
      Questionnaire.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(feedbackQuestionaireView(formWithErrors))
          )
        },
        data => {
          audit(auditType="rasFeedbackSurvey", path=routes.QuestionnaireController.submitQuestionnaire().url,CreateQuestionnaireAudit(data))
          Future.successful(Redirect(routes.QuestionnaireController.feedbackThankyou()))
        }
      )
  }

  def feedbackThankyou: Action[AnyContent] = Action.async { implicit request =>
      Future.successful(Ok(feedbackThankYouView()))
  }

  private def CreateQuestionnaireAudit(survey: Questionnaire): Map[String, String] = {
    Map(
      "easyToUse" -> survey.easyToUse.toString,
      "satisfactionLevel" -> survey.satisfactionLevel.toString,
      "whyGiveThisRating" -> survey.whyGiveThisRating.mkString
    )
  }
}
