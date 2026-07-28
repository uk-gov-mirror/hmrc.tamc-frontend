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

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import services.CacheService.CACHE_EMAIL_ADDRESS
import utils.{EmailAddress, IntegrationSpec}

import scala.concurrent.Future

class ConfirmEmailControllerISpec extends IntegrationSpec {

  "ConfirmEmailController" must {
    "render the confirm email page with the cached email address" in {
      val result = route(app, request("/confirm-email")).value

      status(result) mustBe OK

      val doc = contentAsString(result)
      doc must include("We will email confirmation that you have cancelled your Marriage Allowance within 24 hours.")
      val input = Jsoup.parse(contentAsString(result)).getElementById("transferor-email")
      input.attr("value") mustBe "email@email.com"
    }

    "redirect to the confirm change page when a valid email address is submitted" in {
      when(mockCachingService.put[EmailAddress](eqTo(CACHE_EMAIL_ADDRESS), any())(any(), any()))
        .thenReturn(Future.successful(EmailAddress(email)))

      val fakeRequest = request("/confirm-email", POST)
        .withFormUrlEncodedBody("transferor-email" -> email)

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.UpdateRelationship.routes.ConfirmChangeController.confirmUpdate().url
      )
    }

    "return BAD_REQUEST when an invalid email address is submitted" in {
      val fakeRequest = request("/confirm-email", POST)
        .withFormUrlEncodedBody("transferor-email" -> "some-invalid-email")

      val result = route(app, fakeRequest).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Enter an email address in the correct format, like name@example.com")
    }
  }
}
