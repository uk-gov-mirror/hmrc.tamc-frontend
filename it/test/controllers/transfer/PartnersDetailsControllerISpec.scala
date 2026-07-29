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

import com.github.tomakehurst.wiremock.client.WireMock.{ok, post, urlEqualTo}
import models.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers.*
import services.CacheService.{CACHE_RECIPIENT_DETAILS, CACHE_RECIPIENT_RECORD, USER_ANSWERS_CACHE, USER_ANSWERS_ELIGIBILITY_CHECK}
import uk.gov.hmrc.domain.{Generator, Nino}
import utils.{EmailAddress, IntegrationSpec}

import java.time.LocalDate
import scala.concurrent.Future

class PartnersDetailsControllerISpec extends IntegrationSpec {

  val partnerNino: Nino = new Generator().nextNino

  "PartnersDetailsController" must {

    "render the partners details page with cached recipient details populated" in {
      val recipient = RecipientDetailsFormInput("Test", "User", Gender("F"), partnerNino)

      when(mockCachingService.get[RecipientDetailsFormInput](eqTo(CACHE_RECIPIENT_DETAILS))(any()))
        .thenReturn(Future.successful(Some(recipient)))

      val result = route(app, request("/partners-details")).value

      status(result) mustBe OK
      contentAsString(result) must include("Your partner’s details")
      contentAsString(result) must include("Test")
      contentAsString(result) must include("User")
    }

    "redirect to the eligible years page when valid recipient details are submitted" in {

      val dateOfMarriage: LocalDate            = LocalDate.now().minusDays(10)
      val rdfi                                 = RecipientDetailsFormInput("Test", "User", Gender("F"), partnerNino)
      val recipientData: RegistrationFormInput =
        RegistrationFormInput("Test", "User", Gender("F"), partnerNino, dateOfMarriage)
      val recipientRecord                      = RecipientRecord(userRecord, recipientData, Nil)
      val cacheData                            =
        UserAnswersCacheData(
          None,
          Some(recipientRecord),
          Some(NotificationRecord(EmailAddress("email@email.com"))),
          None,
          None,
          Some(rdfi)
        )

      when(mockCachingService.get(ArgumentMatchers.eq(USER_ANSWERS_CACHE))(any()))
        .thenReturn(Future.successful(Some(cacheData)))

      val eligibilityCache = EligibilityCheckCacheData(None, None, None, None, None)

      when(
        mockCachingService.get[EligibilityCheckCacheData](ArgumentMatchers.eq(USER_ANSWERS_ELIGIBILITY_CHECK))(any())
      ).thenReturn(Future.successful(Some(eligibilityCache)))

      when(mockCachingService.put[RecipientRecord](eqTo(CACHE_RECIPIENT_RECORD), any())(any(), any()))
        .thenReturn(Future.successful(recipientRecord))

      server.stubFor(
        post(urlEqualTo(s"/paye/$generatedNino/get-recipient-relationship"))
          .willReturn(
            ok.withBody(Json.toJson(GetRelationshipResponse(Some(userRecord), None, ResponseStatus("OK"))).toString())
          )
      )

      val fakeRequest = request("/partners-details", POST).withFormUrlEncodedBody(
        "name"      -> "Test",
        "last-name" -> "User",
        "gender"    -> "F",
        "nino"      -> partnerNino.nino
      )

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.EligibleYearsController.eligibleYears().url)
    }

    "return BAD_REQUEST when an invalid form is submitted" in {
      val fakeRequest = request("/partners-details", POST)
        .withFormUrlEncodedBody("name" -> "", "last-name" -> "", "gender" -> "", "nino" -> "")

      val result = route(app, fakeRequest).value

      status(result) mustBe BAD_REQUEST
    }
  }
}
