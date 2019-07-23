/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.eligibility

import config.ApplicationConfig
import controllers.ControllerBaseSpec
import controllers.actions.UnauthenticatedActionTransformer
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers._
import test_utils.MockPermUnauthenticatedAction
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class MarriedOrPartnershipControllerTest extends ControllerBaseSpec {

  ".onPageLoad" should {
    "return success" in {
      val result = sut().onPageLoad()(request)
      status(result) shouldBe OK
    }
  }

  ".onSubmit" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "not a boolean"
        )
        val result = sut().onSubmit()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a success" when {
      "user is not married with permanent auth state" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "false"
        )
        val result = sut(instanceOf[MockPermUnauthenticatedAction]).onSubmit()(request)
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("button-finished").attr("href") shouldBe ApplicationConfig.ptaFinishedUrl
      }

      "user is not married with temporary auth state" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "false"
        )
        val result = sut().onSubmit()(request)
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("button-finished").attr("href") shouldBe ApplicationConfig.gdsFinishedUrl
      }
    }

    "redirect the user" when {
      "user is married" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "true"
        )
        val result = sut().onSubmit()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.eligibility.routes.DateOfBirthController.onPageLoad().url)
      }
    }
  }

  def sut(unAuthAction: UnauthenticatedActionTransformer = instanceOf[UnauthenticatedActionTransformer]): MarriedOrPartnershipController =
    new MarriedOrPartnershipController(
      messagesApi,
      unAuthAction
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

}
