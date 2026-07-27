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

import play.api.test.Helpers.*
import utils.IntegrationSpec

class ClaimsControllerISpec extends IntegrationSpec {

  "ClaimsController" must {

    "render the claims page" in {
      val result = route(app, request("/claims")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("Your Marriage Allowance claims")
      doc must include("Tax year")
      doc must include("Status")
    }
  }
}
