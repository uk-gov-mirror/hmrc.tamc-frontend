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

import models.ApplyForEligibleYears
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.CacheService.CACHE_CHOOSE_YEARS
import uk.gov.hmrc.http.SessionKeys
import utils.IntegrationSpec

import scala.concurrent.Future

class ChooseYearsControllerISpec extends IntegrationSpec {

  "ChooseYearsController" must {

    "render the page with cached selected years" in {

      val result = route(app, request("/choose-years-to-apply-for")).value
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      contentAsString(result) must include("Choose the years you want to apply for")
      doc.getElementById("value_0").hasAttr("checked") mustBe true
      doc.getElementById("value_1").hasAttr("checked") mustBe true
    }

    "render the page with an empty form when no cached years exist" in {

      when(mockCachingService.get[String](eqTo(CACHE_CHOOSE_YEARS))(any()))
        .thenReturn(Future.successful(None))

      val result = route(app, request("/choose-years-to-apply-for")).value

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("value_0").hasAttr("checked") mustBe false
      doc.getElementById("value_1").hasAttr("checked") mustBe false
    }

    "redirect to the partner details page when the current tax year is selected" in {
      val currentTaxYear = ApplyForEligibleYears.CurrentTaxYear.toString

      when(mockCachingService.put[String](eqTo(CACHE_CHOOSE_YEARS), any())(any(), any()))
        .thenReturn(Future.successful(currentTaxYear))

      val request = FakeRequest(POST, s"$baseUrl/choose-years-to-apply-for")
        .withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "Bearer 123"
        )
        .withFormUrlEncodedBody(
          "value[]" -> currentTaxYear
        )

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.PartnersDetailsController.transfer().url)
    }

    "redirect to apply by post when previous tax years are selected" in {
      val previousTaxYears = ApplyForEligibleYears.PreviousTaxYears.toString

      when(
        mockCachingService.put[String](eqTo(CACHE_CHOOSE_YEARS), any())(any(), any())
      ).thenReturn(Future.successful(previousTaxYears))

      val request =
        FakeRequest(POST, s"$baseUrl/choose-years-to-apply-for")
          .withSession(
            SessionKeys.sessionId -> sessionId,
            SessionKeys.authToken -> "Bearer 123"
          )
          .withFormUrlEncodedBody(
            "value[]" -> previousTaxYears
          )

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.ApplyByPostController.applyByPost().url)
    }

    "redirect back to choose years page when an unexpected value is returned from the cache" in {
      when(mockCachingService.put[String](eqTo(CACHE_CHOOSE_YEARS), any())(any(), any()))
        .thenReturn(Future.successful("unexpected-value"))

      val request = FakeRequest(POST, s"$baseUrl/choose-years-to-apply-for")
        .withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "Bearer 123"
        )
        .withFormUrlEncodedBody(
          "value[]" -> ApplyForEligibleYears.CurrentTaxYear.toString
        )

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.ChooseYearsController.chooseYears().url)
    }
  }
}
