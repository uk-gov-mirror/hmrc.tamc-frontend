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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import services.CacheService.CACHE_CHOOSE_YEARS
import utils.IntegrationSpec

import scala.concurrent.Future

class ApplyByPostControllerISpec extends IntegrationSpec {

  "ApplyByPostControllerISpec" must {

    "render the apply by post page with the selected tax years from the cache" in {
      when(mockCachingService.get[String](eqTo(CACHE_CHOOSE_YEARS))(any()))
        .thenReturn(Future.successful(Some("previousTaxYears,currentTaxYear")))

      val result = route(app, request("/apply-by-post")).value

      status(result) mustBe OK
      contentAsString(result) must include("Apply for Marriage Allowance by post")
      contentAsString(result) must include(
        "Your application includes a previous tax year. You cannot apply for previous tax years online."
      )
    }

    "render the apply by post page when no tax years are stored in the cache" in {
      when(mockCachingService.get[String](eqTo(CACHE_CHOOSE_YEARS))(any()))
        .thenReturn(Future.successful(None))

      val result = route(app, request("/apply-by-post")).value

      status(result) mustBe OK
      contentAsString(result) must include("Apply for Marriage Allowance by post")
      contentAsString(result) must include("You cannot apply for previous tax years online.")
    }
  }
}
