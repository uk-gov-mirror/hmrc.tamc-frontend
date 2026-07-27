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

import models.{Gender, RecipientRecord, RegistrationFormInput}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.CacheService.{CACHE_RECIPIENT_RECORD, CACHE_SELECTED_YEARS}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.time.TaxYear
import utils.IntegrationSpec

import java.time.LocalDate
import scala.concurrent.Future

class EligibleYearsControllerISpec extends IntegrationSpec {

  val currentTaxYear: Int   = TaxYear.current.startYear
  val registrationFormInput = RegistrationFormInput(
    "Test",
    "User",
    Gender("M"),
    Nino("AB123456A"),
    LocalDate.now()
  )

  private def recipientRecord(years: List[Int]): RecipientRecord =
    RecipientRecord(userRecord, registrationFormInput, years.map(y => models.TaxYear(y, Some(false))))

  "EligibleYearsController" must {

    "render the eligible years page when the current tax year is available" in {
      when(mockCachingService.get[RecipientRecord](eqTo(CACHE_RECIPIENT_RECORD))(any()))
        .thenReturn(Future.successful(Some(recipientRecord(List(currentTaxYear)))))

      when(mockCachingService.put[List[Int]](eqTo(CACHE_SELECTED_YEARS), any())(any(), any()))
        .thenReturn(Future.successful(List.empty[Int]))

      val result = route(app, request("/eligible-years")).value

      status(result) mustBe OK
      contentAsString(result) must include("Marriage Allowance renews each year unless:")
    }

    "redirect to apply by post when only previous tax years are available" in {
      when(mockCachingService.get[RecipientRecord](eqTo(CACHE_RECIPIENT_RECORD))(any()))
        .thenReturn(Future.successful(Some(recipientRecord(List(currentTaxYear - 2)))))

      when(mockCachingService.put[List[Int]](eqTo(CACHE_SELECTED_YEARS), any())(any(), any()))
        .thenReturn(Future.successful(List.empty[Int]))

      val result = route(app, request("/eligible-years")).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.ApplyByPostController.applyByPost().url)
    }

    "redirect to confirm your email when the current tax year is available" in {
      when(mockCachingService.get[RecipientRecord](eqTo(CACHE_RECIPIENT_RECORD))(any()))
        .thenReturn(Future.successful(Some(recipientRecord(List(currentTaxYear)))))

      when(mockCachingService.put[List[Int]](eqTo(CACHE_SELECTED_YEARS), any())(any(), any()))
        .thenReturn(Future.successful(List(currentTaxYear)))

      val request = FakeRequest(POST, s"$baseUrl/eligible-years")
        .withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.ConfirmEmailController.confirmYourEmail().url)
    }

    "render the no eligible years page when the current tax year is unavailable" in {
      when(mockCachingService.get[RecipientRecord](eqTo(CACHE_RECIPIENT_RECORD))(any()))
        .thenReturn(Future.successful(Some(recipientRecord(Nil))))

      when(mockCachingService.put[List[Int]](eqTo(CACHE_SELECTED_YEARS), any())(any(), any()))
        .thenReturn(Future.successful(Nil))

      val request = FakeRequest(POST, s"$baseUrl/eligible-years")
        .withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) must include("We were unable to process your Marriage Allowance application.")
    }
  }
}
