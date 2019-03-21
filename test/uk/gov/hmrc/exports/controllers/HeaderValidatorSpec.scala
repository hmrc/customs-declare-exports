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

package uk.gov.hmrc.exports.controllers

import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.exports.base.ExportsTestData
import uk.gov.hmrc.exports.models.{Ducr, ErrorResponse, LocalReferenceNumber, ValidatedHeadersSubmissionRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class HeaderValidatorSpec extends UnitSpec with MockitoSugar with ExportsTestData{

  trait SetUp {
    val validator = new HeaderValidator
  }

  "HeaderValidator" should {


    "return LRN from header when extractLRN is called and LRN is present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(ValidHeaders)
      extractedLrn shouldBe Some(declarantLrnValue)
    }

    "return None from header when extractLrnHeader is called header not present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(Map.empty)
      extractedLrn shouldBe None
    }

    "return Ducr from header when extractDucrHeader is called and Ducr is present" in new SetUp {
      val extractedDucr: Option[String] = validator.extractDucrHeader(ValidHeaders)
      extractedDucr shouldBe Some(declarantDucrValue)
    }
    "return None from header when extractDucrHeader is called header not present" in new SetUp {
      val extractedDucr: Option[String] = validator.extractDucrHeader(Map.empty)
      extractedDucr shouldBe None
    }


    "return Mrn from header when extractMrnHeader is called and Mrn is present" in new SetUp {
      val extractedMrn: Option[String] = validator.extractMrnHeader(ValidHeaders)
      extractedMrn shouldBe Some(declarantMrnValue)
    }

    "return None from header when extractMrnHeader is called header not present" in new SetUp {
      val extractedMucr: Option[String] = validator.extractMrnHeader(Map.empty)
      extractedMucr shouldBe None
    }



    "return Right of validatedHeaderResponse when validateHeaders is called on valid headers" in new SetUp {
      implicit val h: Map[String, String] = ValidHeaders
      implicit val hc: HeaderCarrier = mock[HeaderCarrier]

      val result: Either[ErrorResponse, ValidatedHeadersSubmissionRequest] = validator.validateAndExtractSubmissionHeaders
      result should be(Right(ValidatedHeadersSubmissionRequest(LocalReferenceNumber(declarantLrnValue), Ducr(declarantDucrValue))))
    }

    "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
      implicit val h: Map[String, String] = Map("" -> "")
      val result: Either[ErrorResponse, ValidatedHeadersSubmissionRequest] = validator.validateAndExtractSubmissionHeaders
      result should be(Left(ErrorResponse.ErrorInternalServerError))
    }
  }

}
