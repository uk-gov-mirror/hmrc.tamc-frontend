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

import org.jsoup.Jsoup
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, redirectLocation, route, status, writeableOf_AnyContentAsEmpty}
import utils.IntegrationSpec

class ContentChecksSpec extends IntegrationSpec {

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> server.port()
      )
      .build()
  }

  "GET /" must {
    "redirect to /how-it-works" in {
      val result =
        route(app, FakeRequest(GET, "/marriage-allowance-application/")).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe routes.HowItWorksController.howItWorks().url
    }
  }

  "GET /how-it-works" must {
    "render successfully" in {
      val result = route(app, FakeRequest(GET, "/marriage-allowance-application/how-it-works")).value

      status(result) mustBe OK

      val page = Jsoup.parse(contentAsString(result))

      page.title() must include("Apply for Marriage Allowance")
      val text = page.text()
      Seq(
        "Marriage Allowance lets you transfer",
        "Eligibility",
        "Before you apply",
        "Apply now"
      ).foreach(text must include(_))
    }
  }

}
