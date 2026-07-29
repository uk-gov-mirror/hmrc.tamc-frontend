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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.test.Helpers.*
import utils.IntegrationSpec

import scala.concurrent.Future

class FinishedChangeControllerISpec extends IntegrationSpec {

  "FinishedChangeController" must {
    "render the finished page and clear the cache" in {
      when(mockCachingService.clear()(any())).thenReturn(Future.successful(()))

      val result = route(app, request("/finished-change")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("Marriage Allowance cancelled")
      doc must include("You will receive an email acknowledging your cancellation within 24 hours.")
      doc must include("If you do not receive it, please check your spam or junk folder.")

      verify(mockCachingService).clear()(any())
    }
  }
}
