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

package controllers

import play.api.test.Helpers.*
import utils.IntegrationSpec

class HowItWorksControllerISpec extends IntegrationSpec {

  "HowItWorksController" must {

    "redirect /previous-years to /how-it-works" in {
      val result = route(app, request("/previous-years")).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.HowItWorksController.howItWorks().url)
    }

    "render the how it works page when there is no Referer header" in {
      val result = route(app, request("/how-it-works")).value

      status(result) mustBe OK
      contentAsString(result) must include("Marriage Allowance automatically renews at the end of each tax year")
    }

    "redirect to the date of marriage page when Referer contains start-gds url" in {
      val result = route(app, request("/how-it-works").withHeaders(REFERER -> "https://www.gov.uk/")).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe
        Some(controllers.transfer.routes.DateOfMarriageController.dateOfMarriage().url)
    }

    "redirect to the date of marriage page when Referer contains continue-gds url" in {
      val result =
        route(app, request("/how-it-works").withHeaders(REFERER -> "https://www.access.service.gov.uk/")).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe
        Some(controllers.transfer.routes.DateOfMarriageController.dateOfMarriage().url)
    }
  }
}
