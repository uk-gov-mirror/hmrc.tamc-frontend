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

package controllers.transfer

import play.api.test.Helpers.*
import utils.IntegrationSpec

class DoNotApplyControllerISpec extends IntegrationSpec {
  "DoNotApplyController" must {
    "render the do not apply current tax year page" in {
      val result = route(app, request("/dont-apply-current-tax-year")).value

      status(result) mustBe OK

      contentAsString(result) must include("You don't want to apply for the current tax year onwards")
    }
  }
}
