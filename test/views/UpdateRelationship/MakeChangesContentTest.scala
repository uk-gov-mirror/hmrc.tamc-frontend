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

package views.UpdateRelationship

import forms.coc.*
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, CreateRelationshipRecordsHelper, NinoGenerator}
import views.html.coc.reason_for_change

import java.util.Locale

class MakeChangesContentTest extends BaseTest with Injecting with NinoGenerator with CreateRelationshipRecordsHelper {

  val view: reason_for_change = inject[reason_for_change]
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  lazy val nino: String = generateNino().nino

  private val relationshipRecords = createRelationshipRecords()
  val doc: Document = Jsoup.parse(view(MakeChangesDecisionForm.form(), relationshipRecords.primaryRecord.role).toString())

  "Make change - get view" should {

    "display correct pageHeading" in {
      doc.getElementById("pageHeading").text() shouldBe "Why do you need to stop your Marriage Allowance?"
    }

    "render the hidden user type tag" in {
      doc.select("#ma-user-type").size() shouldBe 1
      doc.select("#ma-user-type").attr("data-user-type") shouldBe "transferor"
    }

    "show all appropriate radio buttons" in {
      doc.getElementsByClass("govuk-label govuk-radios__label").eachText().toArray() shouldBe Array(
        "Do not need Marriage Allowance any more",
        "Divorce, end of civil partnership or legally separated",
        "Bereavement"
      )
    }

    "Display continue button" in {
      doc.getElementById("submit").text() shouldBe "Continue"
    }

    "Display all correct page text" in {
      doc.getElementsByTag("p").eachText().toArray() shouldBe Array("Beta This is a new service. Help us improve it and give your feedback (opens in new tab).")
    }
  }

}
