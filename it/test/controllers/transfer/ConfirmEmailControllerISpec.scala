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

import models.NotificationRecord
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.CacheService.CACHE_NOTIFICATION_RECORD
import uk.gov.hmrc.http.SessionKeys
import utils.{EmailAddress, IntegrationSpec}

import scala.concurrent.Future

class ConfirmEmailControllerISpec extends IntegrationSpec {

  "ConfirmEmailController" must {

    "render the confirm email page with the cached email address" in {
      when(mockCachingService.get[NotificationRecord](eqTo(CACHE_NOTIFICATION_RECORD))(any()))
        .thenReturn(Future.successful(Some(NotificationRecord(EmailAddress("email@email.com")))))

      val result = route(app, request("/confirm-your-email")).value

      status(result) mustBe OK

      val input = Jsoup.parse(contentAsString(result)).getElementById("transferor-email")
      input.attr("value") mustBe "email@email.com"
      contentAsString(result) must include("We will email confirmation of your Marriage Allowance application.")
    }

    "render the confirm email page when no email address is cached" in {
      when(mockCachingService.get[NotificationRecord](eqTo(CACHE_NOTIFICATION_RECORD))(any()))
        .thenReturn(Future.successful(None))

      val result = route(app, request("/confirm-your-email")).value

      status(result) mustBe OK
      contentAsString(result) must include("We will email confirmation of your Marriage Allowance application.")
      contentAsString(result) must not include """value="email@email.com""""
    }

    "redirect to the confirmation page when a valid email address is submitted" in {
      when(mockCachingService.put[NotificationRecord](eqTo(CACHE_NOTIFICATION_RECORD), any())(any(), any()))
        .thenReturn(Future.successful(NotificationRecord(EmailAddress("email@email.com"))))

      val request = FakeRequest(POST, s"$baseUrl/confirm-your-email")
        .withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "Bearer 123"
        )
        .withFormUrlEncodedBody(
          "transferor-email" -> "email@email.com"
        )

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.ConfirmController.confirm().url)
    }

    "return a BAD_REQUEST when an invalid email address is submitted" in {
      val request = FakeRequest(POST, s"$baseUrl/confirm-your-email")
        .withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "Bearer 123"
        )
        .withFormUrlEncodedBody(
          "transferor-email" -> "some invalid-email"
        )

      val result = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Enter an email address in the correct format, like name@example.com")
    }
  }
}
