/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.eligibility

import config.ApplicationConfig.{gdsFinishedUrl, ptaFinishedUrl}
import controllers.BaseController
import controllers.actions.UnauthenticatedActionTransformer
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import views.html.multiyear.eligibility_check

class MarriedOrPartnershipController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                unauthenticatedAction: UnauthenticatedActionTransformer
                                              )(implicit templateRenderer: TemplateRenderer,
                                                formPartialRetriever: FormPartialRetriever) extends BaseController {


  def onPageLoad(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(eligibility_check(eligibilityCheckForm = eligibilityForm))
  }

  def onSubmit(): Action[AnyContent] = {

    def finishUrl(isLoggedIn: Boolean): String =
      if (isLoggedIn) ptaFinishedUrl else gdsFinishedUrl

    unauthenticatedAction {
      implicit request =>
        eligibilityForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(views.html.multiyear.eligibility_check(formWithErrors)),
          eligibilityInput => {
            if (eligibilityInput.married) {
              Redirect(controllers.eligibility.routes.DateOfBirthController.onPageLoad())
            } else {
              Ok(views.html.multiyear.eligibility_non_eligible_finish(finishUrl(request.authState.permanent)))
            }
          })
    }
  }
}
