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
import play.api.libs.json._
import uk.gov.hmrc.exports.models.declaration.submissions.{CancellationRequestExists, CancellationRequested, CancellationStatus, MissingDeclaration}

class CancellationStatusSpec extends WordSpec with MustMatchers {

  "Cancellation Status Reads" should {

    "read value correctly" in {

      import uk.gov.hmrc.exports.models.declaration.submissions.CancellationStatus.CancellationStatusReads.reads

      reads(JsString("CancellationRequestExists")) must be(JsSuccess(CancellationRequestExists))
      reads(JsString("CancellationRequested")) must be(JsSuccess(CancellationRequested))
      reads(JsString("MissingDeclaration")) must be(JsSuccess(MissingDeclaration))
      reads(JsString("IncorrectStatus")) must be(JsError("Incorrect cancellation status"))
    }
  }

  "Cancellation Status Writes" should {

    "write value correctly" in {

      import uk.gov.hmrc.exports.models.declaration.submissions.CancellationStatus.CancellationStatusWrites.writes

      writes(CancellationRequestExists) must be(JsString("CancellationRequestExists"))
      writes(CancellationRequested) must be(JsString("CancellationRequested"))
      writes(MissingDeclaration) must be(JsString("MissingDeclaration"))
    }
  }

  "Cancellation Status unapply" should {

    "correctly unapply CancellationStatus object" in {

      val expectedResult =
        Some(CancellationRequestExists.productPrefix -> Json.toJson(CancellationRequestExists.toString))

      CancellationStatus.unapply(CancellationRequestExists) must be(expectedResult)
    }
  }
}
