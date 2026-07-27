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

import com.github.tomakehurst.wiremock.client.WireMock.{ok, put, urlEqualTo}
import models.*
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.CacheService.USER_ANSWERS_CACHE
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.time.TaxYear
import utils.{EmailAddress, IntegrationSpec}

import java.time.LocalDate
import scala.concurrent.Future

class ConfirmControllerISpec extends IntegrationSpec {

  private val dateOfMarriage  = LocalDate.now().minusDays(10)
  private val partnerNino     = new Generator().nextNino
  private val recipientData   = RegistrationFormInput("Claire", "Forester", Gender("F"), partnerNino, dateOfMarriage)
  private val recipientRecord = RecipientRecord(userRecord, recipientData, Nil)

  private val cacheData = UserAnswersCacheData(
    transferor = Some(userRecord),
    recipient = Some(recipientRecord),
    notification = Some(NotificationRecord(EmailAddress("email@email.com"))),
    relationshipCreated = None,
    selectedYears = Some(List(TaxYear.current.startYear)),
    recipientDetailsFormData = Some(RecipientDetailsFormInput("Claire", "Forester", Gender("F"), partnerNino)),
    dateOfMarriage = Some(DateOfMarriageFormInput(dateOfMarriage))
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockCachingService.get[UserAnswersCacheData](eqTo(USER_ANSWERS_CACHE))(any()))
      .thenReturn(Future.successful(Some(cacheData)))
  }

  "ConfirmController" must {
    "render the confirmation page" in {
      val result = route(app, request("/confirm")).value

      status(result) mustBe OK
      contentAsString(result) must include("Check your answers before sending your application")

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("recipient-name").text() mustBe "Claire Forester"
      document.getElementById("transferor-email").text() mustBe "email@email.com"
    }

    "redirect to the finished page when the application is submitted" in {
      server.stubFor(
        put(urlEqualTo(s"/paye/$generatedNino/create-multi-year-relationship/pta"))
          .willReturn(ok.withBody(Json.toJson(CreateRelationshipResponse(ResponseStatus("OK"))).toString()))
      )

      val request = FakeRequest(POST, s"$baseUrl/confirm")
        .withSession(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "Bearer 123"
        )

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.transfer.routes.FinishedController.finished().url)
    }
  }
}
