/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.controllers.request

import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration._

class ExportsDeclarationRequestSpec extends UnitSpec {

  "ExportsDeclarationRequest" should {
    "have json format that parse declaration in version 2" in {
      Json
        .parse(ExportsDeclarationSpec.exportsDeclarationRequestAsString)
        .validate[ExportsDeclarationRequest]
        .fold(error => fail(s"Could not parse - $error"), declaration => {
          declaration.transport.borderModeOfTransportCode mustNot be(empty)
          declaration.transport.meansOfTransportOnDepartureType mustNot be(empty)
          declaration.transport.transportPayment mustNot be(empty)
        })
    }
  }
}
