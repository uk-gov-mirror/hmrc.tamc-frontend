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

package utils

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, post, urlEqualTo}
import models.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.test.Injecting
import services.CacheService.*
import services.CachingService
import uk.gov.hmrc.domain.{AtedUtr, Generator, Nino}
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait IntegrationSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with Matchers
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with Injecting {

  val generatedNino: Nino     = new Generator().nextNino
  val generatedSaUtr: AtedUtr = new Generator().nextAtedUtr

  lazy val messagesApi: MessagesApi    = inject[MessagesApi]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  val taxYear: Int       = TaxYear.current.startYear
  val instanceIdentifier = 1
  val timeStamp          = "20130101"
  val firstName          = "First"
  val surname            = "Surname"
  val date: LocalDate    = LocalDate.now()
  val email              = "email@email.com"

  val activeRecipientRelationshipRecord: RelationshipRecord = RelationshipRecord(
    Recipient.value,
    creationTimestamp = "20150531",
    participant1StartDate = "20150531",
    relationshipEndReason = Some(DesRelationshipEndReason.Default),
    participant1EndDate = None,
    otherParticipantInstanceIdentifier = "123456789123",
    otherParticipantUpdateTimestamp = "20150531"
  )

  val recordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), Some(createLoggedInUserInfo()))

  val mockCachingService: CachingService = mock[CachingService]

  lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def createLoggedInUserInfo(
    name: Option[CitizenName] = Some(CitizenName(Some(firstName), Some(surname)))
  ): LoggedInUserInfo = LoggedInUserInfo(instanceIdentifier, timeStamp, None, name)

  val relationshipRecordList = RelationshipRecordList(
    Seq(activeRecipientRelationshipRecord),
    Some(createLoggedInUserInfo())
  )

  val maConnectorResponse = RelationshipRecordStatusWrapper(
    relationship_record = relationshipRecordList,
    status = ResponseStatus("OK")
  )


  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[CachingService].toInstance(mockCachingService)
      )
      .configure(
        "microservice.services.auth.port"               -> server.port(),
        "microservice.services.pertax-auth.port"        -> server.port(),
        "microservice.services.marriage-allowance.port" -> server.port()
      )
      .build()

  override def beforeEach(): Unit = {

    super.beforeEach()

    val authResponse =
      s"""
         |{
         |    "confidenceLevel": 200,
         |    "nino": "$generatedNino",
         |    "saUtr": "$generatedSaUtr",
         |    "name": {
         |        "name": "John",
         |        "lastName": "Smith"
         |    },
         |    "loginTimes": {
         |        "currentLogin": "2021-06-07T10:52:02.594Z",
         |        "previousLogin": null
         |    },
         |    "optionalCredentials": {
         |        "providerId": "4911434741952698",
         |        "providerType": "GovernmentGateway"
         |    },
         |    "authProviderId": {
         |        "ggCredId": "xyz"
         |    },
         |    "externalId": "testExternalId"
         |}
         |""".stripMargin

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(aResponse().withBody(authResponse))
    )

    server.stubFor(
      post(urlEqualTo("/pertax/authorise"))
        .willReturn(aResponse().withBody("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
    )

    server.stubFor(
      get(urlEqualTo(s"/paye/$generatedNino/list-relationship"))
        .willReturn(
          ok.withBody(Json.toJson(maConnectorResponse).toString())
        )
    )

    val relationshipRecords                = RelationshipRecords(recordList, date)
    val maEndingDates                      = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
    val dateOfMarriageInput                = DateOfMarriageFormInput(LocalDate.now().minusYears(2))
    val confirmationUpdateAnswersCacheData = ConfirmationUpdateAnswersCacheData(
      Some(relationshipRecords),
      Some(date),
      Some("email@email.com"),
      Some(maEndingDates)
    )

    val recipientDetails                   = RecipientDetailsFormInput("Firstname", "Lastname", Gender("M"), Nino("AB123456A"))
    val citizenName: CitizenName = CitizenName(Some("Test"), Some("User"))
    val userRecord: UserRecord = UserRecord(123456789L, "2015", None, Some(citizenName))


    when(mockCachingService.get[RelationshipRecords](eqTo(CACHE_RELATIONSHIP_RECORDS))(any()))
      .thenReturn(Future.successful(Some(relationshipRecords)))

    when(mockCachingService.get[LocalDate](eqTo(CACHE_DIVORCE_DATE))(any()))
      .thenReturn(Future.successful(Some(date)))

    when(mockCachingService.get[DateOfMarriageFormInput](eqTo(CACHE_MARRIAGE_DATE))(any()))
      .thenReturn(Future.successful(Some(dateOfMarriageInput)))

    when(mockCachingService.get[String](eqTo(CACHE_CHOOSE_YEARS))(any()))
      .thenReturn(Future.successful(Some("2018,2019")))

    when(mockCachingService.get[String](eqTo(CACHE_CHECK_CLAIM_OR_CANCEL))(any()))
      .thenReturn(Future.successful(Some("checkMarriageAllowanceClaim")))

    when(mockCachingService.get[String](eqTo(CACHE_MAKE_CHANGES_DECISION))(any()))
      .thenReturn(Future.successful(Some("Divorce")))

    when(mockCachingService.get[ConfirmationUpdateAnswersCacheData](eqTo(USER_ANSWERS_UPDATE_CONFIRMATION))(any()))
      .thenReturn(Future.successful(Some(confirmationUpdateAnswersCacheData)))

    when(mockCachingService.get[NotificationRecord](eqTo(CACHE_NOTIFICATION_RECORD))(any()))
      .thenReturn(Future.successful(Some(NotificationRecord(EmailAddress(email)))))

    when(mockCachingService.get[EmailAddress](eqTo(CACHE_EMAIL_ADDRESS))(any()))
      .thenReturn(Future.successful(Some(EmailAddress(email))))

    when(mockCachingService.get[RecipientDetailsFormInput](eqTo(CACHE_RECIPIENT_DETAILS))(any()))
      .thenReturn(Future.successful(Some(recipientDetails)))

    when(mockCachingService.put[MarriageAllowanceEndingDates](eqTo(CACHE_MA_ENDING_DATES), any())(any(), any()))
      .thenReturn(Future.successful(maEndingDates))

    when(mockCachingService.put[Boolean](eqTo(CACHE_LOCKED_CREATE), any())(any(), any()))
      .thenReturn(Future.successful(false))

    when(mockCachingService.put[UserRecord](eqTo(CACHE_TRANSFEROR_RECORD), any())(any(), any()))
      .thenReturn(Future.successful(userRecord))

    when(mockCachingService.put[RelationshipRecords](eqTo(CACHE_RELATIONSHIP_RECORDS), any())(any(), any()))
      .thenReturn(Future.successful(relationshipRecords))

    when(mockCachingService.clear()(any())).thenReturn(Future.successful(()))
  }
}
