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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import services.CacheService.CACHE_DIVORCE_DATE
import utils.IntegrationSpec

import java.time.LocalDate
import scala.concurrent.Future

class DivorceControllerISpec extends IntegrationSpec {

  "DivorceController" must {

    "render the divorce year page" in {
      val result = route(app, request("/divorce-enter-year")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("Date of divorce, end of civil partnership or legal separation")
      doc must include(
        "You need to go to court to be legally separated. You can still get Marriage Allowance if you are separated, but not legally separated."
      )
    }

    "redirect to the divorce explanation page when a valid date is submitted" in {
      when(mockCachingService.put[LocalDate](eqTo(CACHE_DIVORCE_DATE), any())(any(), any()))
        .thenReturn(Future.successful(date))

      val fakeRequest = request("/divorce-enter-year", POST)
        .withFormUrlEncodedBody(
          "dateOfDivorce.year"  -> date.getYear.toString,
          "dateOfDivorce.month" -> date.getMonthValue.toString,
          "dateOfDivorce.day"   -> date.getDayOfMonth.toString
        )

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.UpdateRelationship.routes.DivorceController.divorceEndExplanation().url
      )
    }

    "return BAD_REQUEST when an invalid divorce date is submitted" in {
      val fakeRequest = request("/divorce-enter-year", POST)
        .withFormUrlEncodedBody(
          "dateOfDivorce.year"  -> "",
          "dateOfDivorce.month" -> "",
          "dateOfDivorce.day"   -> ""
        )

      val result = route(app, fakeRequest).value

      status(result) mustBe BAD_REQUEST

      contentAsString(result) must include("Enter your date of divorce, end of civil partnership or legal separation")
    }

    "render the divorce explanation page" in {
      val result = route(app, request("/divorce-end-explanation")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("Cancelling Marriage Allowance")
      doc must include("You have told us you divorced, ended your civil partnership or were legally separated")
    }
  }
}
