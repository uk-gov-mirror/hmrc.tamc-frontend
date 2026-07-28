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

package controllers.updateRelationship

import forms.coc.CheckClaimOrCancelDecisionForm
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import services.CacheService.CACHE_CHECK_CLAIM_OR_CANCEL
import utils.IntegrationSpec

import scala.concurrent.Future

class ChooseControllerISpec extends IntegrationSpec {

  "ChooseController" must {

    "render the decision page" in {
      val result = route(app, request("/choose")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("What do you want to do?")
      doc must include("Check current or earlier Marriage Allowance claims")
      doc must include("Stop current Marriage Allowance claim")
    }

    "redirect to the claims page when Check Marriage Allowance is selected" in {
      when(mockCachingService.put[String](eqTo(CACHE_CHECK_CLAIM_OR_CANCEL), any())(any(), any()))
        .thenReturn(Future.successful("checkMarriageAllowanceClaim"))

      val fakeRequest = request("/choose", POST)
        .withFormUrlEncodedBody(
          CheckClaimOrCancelDecisionForm.DecisionChoice -> CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim
        )

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.UpdateRelationship.routes.ClaimsController.claims().url)
    }

    "redirect to the make change page when Stop Marriage Allowance is selected" in {
      when(mockCachingService.put[String](eqTo(CACHE_CHECK_CLAIM_OR_CANCEL), any())(any(), any()))
        .thenReturn(Future.successful("stopMarriageAllowance"))

      val fakeRequest = request("/choose", POST)
        .withFormUrlEncodedBody(
          CheckClaimOrCancelDecisionForm.DecisionChoice -> CheckClaimOrCancelDecisionForm.StopMarriageAllowance
        )

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.UpdateRelationship.routes.MakeChangesController.makeChange().url)
    }

    "return BAD_REQUEST and show form errors when no option is selected" in {
      val fakeRequest = request("/choose", POST)
        .withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> "")

      val result = route(app, fakeRequest).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Select if you want to check or stop your marriage allowance claim")
    }
  }
}
