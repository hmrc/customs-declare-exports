/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.connector.ead

import play.api.test.Helpers._
import stubs.CustomsDeclarationsInformationAPIConfig._
import stubs.CustomsDeclarationsInformationAPIService
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.connectors.ead.CustomsDeclarationsInformationConnector
import uk.gov.hmrc.http.InternalServerException

class CustomsDeclarationsInformationConnectorSpec extends IntegrationTestSpec with CustomsDeclarationsInformationAPIService {

  private lazy val connector = inject[CustomsDeclarationsInformationConnector]

  "Customs Declarations Information Connector" should {
    val mrn = "18GB9JLC3CU1LFGVR2"

    "return response with specific status" when {
      "request is processed successfully - 200" in {
        startService(OK, mrn)
        val mrnStatus = connector.fetchMrnStatus(mrn).futureValue.get
        mrnStatus.mrn mustBe mrn
        mrnStatus.eori mustBe "GB123456789012000"
        verifyDecServiceWasCalledCorrectly(mrn, expectedApiVersion = apiVersion, bearerToken)
      }

      "request is not processed - 500" in {

        startService(INTERNAL_SERVER_ERROR, mrn)
        intercept[InternalServerException] {
          await(connector.fetchMrnStatus(mrn))
        }
      }
    }
  }
}
