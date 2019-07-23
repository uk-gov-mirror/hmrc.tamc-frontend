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

import controllers.ControllerBaseSpec
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers._

class DateOfBirthControllerTest extends ControllerBaseSpec {

  ".onPageLoad" should {
    "return success" in {
      val result = sut.onPageLoad()(request)
      status(result) shouldBe OK
    }
  }

  ".onSubmit" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "date-of-birth" -> "not bool")
        val result = sut.onSubmit()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to do you live in scotland" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "date-of-birth" -> "true"
        )
        val result = sut.onSubmit()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.doYouLiveInScotland().url)
      }
    }
  }

  def sut: DateOfBirthController = instanceOf[DateOfBirthController]
}
