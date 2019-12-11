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

package unit.uk.gov.hmrc.exports.models.declaration

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsError, JsString, JsSuccess}
import uk.gov.hmrc.exports.models.declaration.ModeOfTransport

class ModeOfTransportSpec extends WordSpec with MustMatchers {
  "Mode of Transport" must {
    Seq("1", "2", "3", "4", "5", "7", "8", "9").foreach { code =>
      s"have format for mode '$code'" in {
        JsString(code).validate[ModeOfTransport] mustBe a [JsSuccess[_]]
      }
    }

    "format for unknown value return error" in {
      JsString("unknown").validate[ModeOfTransport] mustBe JsError("error.value.unknown")
    }
  }
}
