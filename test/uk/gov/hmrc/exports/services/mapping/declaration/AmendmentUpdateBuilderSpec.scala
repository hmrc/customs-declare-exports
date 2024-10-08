/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.base.UnitSpec
import wco.datamodel.wco.dec_dms._2.Declaration

class AmendmentUpdateBuilderSpec extends UnitSpec {

  private val builder = new AmendmentUpdateBuilder()

  "Build then add" should {
    "append to declaration" in {
      val amendment = new Declaration.Amendment

      builder.buildThenAdd("reason", amendment)

      amendment.getChangeReasonCode.getValue mustBe "reason"
    }
  }

}
