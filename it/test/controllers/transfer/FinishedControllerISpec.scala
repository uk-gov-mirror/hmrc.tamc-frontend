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

import models.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.test.Helpers.*
import services.CacheService.USER_ANSWERS_CACHE
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.time.TaxYear
import utils.{EmailAddress, IntegrationSpec}

import java.time.LocalDate
import scala.concurrent.Future

class FinishedControllerISpec extends IntegrationSpec {

  private val dateOfMarriage  = LocalDate.now().minusDays(10)
  private val partnerNino     = new Generator().nextNino
  private val recipientData   = RegistrationFormInput("Claire", "Forester", Gender("F"), partnerNino, dateOfMarriage)
  private val recipientRecord = RecipientRecord(userRecord, recipientData, Nil)

  private val cacheData = UserAnswersCacheData(
    transferor = Some(userRecord),
    recipient = Some(recipientRecord),
    notification = Some(NotificationRecord(EmailAddress("email@email.com"))),
    relationshipCreated = Some(true),
    selectedYears = Some(List(TaxYear.current.startYear)),
    recipientDetailsFormData = Some(RecipientDetailsFormInput("Claire", "Forester", Gender("F"), partnerNino)),
    dateOfMarriage = Some(DateOfMarriageFormInput(dateOfMarriage))
  )

  "FinishedController" must {
    "render the finished page and clear the cache" in {

      when(mockCachingService.get[UserAnswersCacheData](eqTo(USER_ANSWERS_CACHE))(any()))
        .thenReturn(Future.successful(Some(cacheData)))

      when(mockCachingService.clear()(any())).thenReturn(Future.successful(()))

      val result = route(app, request("/finished")).value

      status(result) mustBe OK

      val doc = contentAsString(result)
      doc must include("Marriage Allowance application complete")
      doc must include("A confirmation email will be sent to")
      doc must include("email@email.com")

      verify(mockCachingService).clear()(any())
    }
  }
}
