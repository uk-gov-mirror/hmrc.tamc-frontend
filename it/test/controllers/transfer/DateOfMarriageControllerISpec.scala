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

import models.DateOfMarriageFormInput
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import services.CacheService.CACHE_MARRIAGE_DATE
import utils.IntegrationSpec

import java.time.LocalDate
import scala.concurrent.Future

class DateOfMarriageControllerISpec extends IntegrationSpec {

  "DateOfMarriageController" must {

    "redirect old transfer-allowance route to /date-of-marriage" in {
      val result = route(app, request("/transfer-allowance")).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.transfer.routes.DateOfMarriageController.dateOfMarriage().url
      )
    }

    "render the date of marriage page when no date has been cached" in {
      when(mockCachingService.get[DateOfMarriageFormInput](eqTo(CACHE_MARRIAGE_DATE))(any()))
        .thenReturn(Future.successful(None))

      val result = route(app, request("/date-of-marriage")).value

      status(result) mustBe OK
      contentAsString(result) must include("Date of marriage or civil partnership")
    }

    "render the date of marriage page with the cached date populated" in {
      val cachedDate = DateOfMarriageFormInput(LocalDate.of(2020, 6, 15))
      when(mockCachingService.get[DateOfMarriageFormInput](eqTo(CACHE_MARRIAGE_DATE))(any()))
        .thenReturn(Future.successful(Some(cachedDate)))

      val result = route(app, request("/date-of-marriage")).value

      status(result) mustBe OK
      contentAsString(result) must include("Date of marriage or civil partnership")
      contentAsString(result) must include("2020")
    }

    "redirect to the partners details page when a current tax year date is submitted" in {
      val today = LocalDate.now()

      val fakeRequest = request("/date-of-marriage", POST)
        .withFormUrlEncodedBody(
          "dateOfMarriage.day"   -> today.getDayOfMonth.toString,
          "dateOfMarriage.month" -> today.getMonthValue.toString,
          "dateOfMarriage.year"  -> today.getYear.toString
        )

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.PartnersDetailsController.transfer().url)
    }

    "redirect to the choose years page when a previous tax year date is submitted" in {
      val previousTaxYearDate = LocalDate.now().minusYears(2)

      val fakeRequest = request("/date-of-marriage", POST)
        .withFormUrlEncodedBody(
          "dateOfMarriage.day"   -> previousTaxYearDate.getDayOfMonth.toString,
          "dateOfMarriage.month" -> previousTaxYearDate.getMonthValue.toString,
          "dateOfMarriage.year"  -> previousTaxYearDate.getYear.toString
        )

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.ChooseYearsController.chooseYears().url)
    }

    "return BAD_REQUEST when an invalid date of marriage is submitted" in {
      val fakeRequest = request("/date-of-marriage", POST)
        .withFormUrlEncodedBody(
          "dateOfMarriage.day"   -> "",
          "dateOfMarriage.month" -> "",
          "dateOfMarriage.year"  -> ""
        )

      val result = route(app, fakeRequest).value

      status(result) mustBe BAD_REQUEST
    }
  }
}
