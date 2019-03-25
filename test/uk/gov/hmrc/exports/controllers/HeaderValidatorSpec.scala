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
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

class HeaderValidatorSpec extends UnitSpec with MockitoSugar with ExportsTestData{

  trait SetUp {
    val validator = new HeaderValidator
  }

  "HeaderValidator" should {

    "return lrn from header when extract is called and header is present" in new SetUp {
      val extractedLrn: Option[String] =
        validator.extractLrnHeader(ValidHeaders)
      extractedLrn shouldBe Some(declarantLrnValue)
    }

    "return ducr from header when extract is called and header is present" in new SetUp {
      val extractedDucr: Option[String] =
        validator.extractDucrHeader(ValidHeaders)
      extractedDucr shouldBe Some(declarantDucrValue)
    }

    "return mrn from header when extract is called and header is present" in new SetUp {
      val extractedMrn: Option[String] =
        validator.extractMrnHeader(ValidHeaders)
      extractedMrn shouldBe Some(declarantMrnValue)
    }

    "return eori from header when extract is called and header is present" in new SetUp {
      val extractedEori: Option[String] =
        validator.extractEoriHeader(ValidHeaders)
      extractedEori shouldBe Some(declarantEoriValue)
    }

    "return authToken from header when extract is called and header is present" in new SetUp {
      val extractedAuthToken: Option[String] =
        validator.extractAuthTokenHeader(ValidHeaders)
      extractedAuthToken shouldBe Some(dummyToken)
    }

    "return conversationId from header when extract is called and header is present" in new SetUp {
      val extractedConversationId: Option[String] =
        validator.extractConversationIdHeader(ValidHeaders)
      extractedConversationId shouldBe Some(conversationId)
    }

    "return None from header when extract is called and LRN header not present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(Map.empty)
      extractedLrn shouldBe None
    }

    "return None from header when extract is called and DUCR header not present" in new SetUp {
      val extractedDucr: Option[String] = validator.extractDucrHeader(Map.empty)
      extractedDucr shouldBe None
    }

    "return None from header when extract is called and MUCR header not present" in new SetUp {
      val extractedMucr: Option[String] = validator.extractMrnHeader(Map.empty)
      extractedMucr shouldBe None
    }

    "return None from header when extract is called and EORI header not present" in new SetUp {
      val extractedEori: Option[String] = validator.extractEoriHeader(Map.empty)
      extractedEori shouldBe None
    }

    "return None from header when extract is called and AuthToken header not present" in new SetUp {
      val extractedAuthToken: Option[String] =
        validator.extractAuthTokenHeader(Map.empty)
      extractedAuthToken shouldBe None
    }

    "return None from header when extract is called and conversationId header not present" in new SetUp {
      val extractedConversationId: Option[String] =
        validator.extractConversationIdHeader(Map.empty)
      extractedConversationId shouldBe None
    }

    "validateSubmissionHeaders" should {

      "return Right of validatedHeaderResponse when validateHeaders is called on valid headers" in new SetUp {
        implicit val hc: HeaderCarrier = mock[HeaderCarrier]

        val result: Either[ErrorResponse, ValidatedHeadersSubmissionRequest] =
          validator.validateAndExtractSubmissionHeaders(ValidHeaders)
        result should be(
          Right(
            ValidatedHeadersSubmissionRequest(
              LocalReferenceNumber(declarantLrnValue),
              Ducr(declarantDucrValue))))
      }

      "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
        val result: Either[ErrorResponse, ValidatedHeadersSubmissionRequest] =
          validator.validateAndExtractSubmissionHeaders(Map.empty)
        result shouldBe Left(ErrorResponse.ErrorInvalidPayload)
      }

    }

    "validateCancellationHeaders" should {

      "return Right of validatedHeaderResponse when validateHeaders is called on valid headers" in new SetUp {
        implicit val hc: HeaderCarrier = mock[HeaderCarrier]

        val result: Either[ErrorResponse, ValidatedHeadersCancellationRequest] =
          validator.validateAndExtractCancellationHeaders(ValidHeaders)
        result should be(
          Right(ValidatedHeadersCancellationRequest(Mrn(declarantMrnValue))))
      }

      "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
        val result: Either[ErrorResponse, ValidatedHeadersCancellationRequest] =
          validator.validateAndExtractCancellationHeaders(Map.empty)
        result should be(Left(ErrorResponse.ErrorInvalidPayload))
      }

    }

    "validateNotificationHeaders" should {

      "return Right of validatedHeaderResponse when validateHeaders is called on valid headers" in new SetUp {
        implicit val hc: HeaderCarrier = mock[HeaderCarrier]

        val result
          : Either[ErrorResponse, ValidatedHeadersNotificationApiRequest] =
          validator.validateAndExtractNotificationHeaders(ValidHeaders)
        result should be(Right(
          ValidatedHeadersNotificationApiRequest(AuthToken(dummyToken),
                                                 ConversationId(conversationId),
                                                 Eori(declarantEoriValue))))
      }

      "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
        val result
          : Either[ErrorResponse, ValidatedHeadersNotificationApiRequest] =
          validator.validateAndExtractNotificationHeaders(Map.empty)
        result should be(Left(ErrorResponse.ErrorInvalidPayload))
      }

    }
  }

}
