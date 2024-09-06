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

package uk.gov.hmrc.exports.controllers

import testdata.ExportsTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.models._

class HeaderValidatorSpec extends UnitSpec {

  trait SetUp {
    val validator = new HeaderValidator
  }

  "HeaderValidator" should {

    "return lrn from header when extract is called and header is present" in new SetUp {
      val extractedLrn: Option[String] =
        validator.extractLrnHeader(ValidHeaders)
      extractedLrn must equal(Some(declarantLrnValue))
    }

    "return ducr from header when extract is called and header is present" in new SetUp {
      val extractedDucr: Option[String] =
        validator.extractOptionalDucrHeader(ValidHeaders)
      extractedDucr must equal(Some(declarantDucrValue))
    }

    "return mrn from header when extract is called and header is present" in new SetUp {
      val extractedMrn: Option[String] =
        validator.extractMrnHeader(ValidHeaders)
      extractedMrn must equal(Some(declarantMrnValue))
    }

    "return eori from header when extract is called and header is present" in new SetUp {
      val extractedEori: Option[String] =
        validator.extractEoriHeader(ValidHeaders)
      extractedEori must equal(Some(declarantEoriValue))
    }

    "return authToken from header when extract is called and header is present" in new SetUp {
      val extractedAuthToken: Option[String] =
        validator.extractAuthTokenHeader(ValidHeaders)
      extractedAuthToken must equal(Some(dummyToken))
    }

    "return conversationId from header when extract is called and header is present" in new SetUp {
      val extractedConversationId: Option[String] =
        validator.extractConversationIdHeader(ValidHeaders)
      extractedConversationId must equal(Some(actionId))
    }

    "return None from header when extract is called and LRN header not present" in new SetUp {
      val extractedLrn: Option[String] = validator.extractLrnHeader(Map.empty)
      extractedLrn must equal(None)
    }

    "return None from header when extract is called and DUCR header not present" in new SetUp {
      val extractedDucr: Option[String] = validator.extractOptionalDucrHeader(Map.empty)
      extractedDucr must equal(None)
    }

    "return None from header when extract is called and MUCR header not present" in new SetUp {
      val extractedMucr: Option[String] = validator.extractMrnHeader(Map.empty)
      extractedMucr must equal(None)
    }

    "return None from header when extract is called and EORI header not present" in new SetUp {
      val extractedEori: Option[String] = validator.extractEoriHeader(Map.empty)
      extractedEori must equal(None)
    }

    "return None from header when extract is called and AuthToken header not present" in new SetUp {
      val extractedAuthToken: Option[String] =
        validator.extractAuthTokenHeader(Map.empty)
      extractedAuthToken must equal(None)
    }

    "return None from header when extract is called and conversationId header not present" in new SetUp {
      val extractedConversationId: Option[String] =
        validator.extractConversationIdHeader(Map.empty)
      extractedConversationId must equal(None)
    }

    "validateSubmissionHeaders" should {

      "return Right of validatedHeaderResponse when validateHeaders is called on valid headers" in new SetUp {
        val result: Either[ErrorResponse, SubmissionRequestHeaders] =
          validator.validateAndExtractSubmissionHeaders(ValidHeaders)
        result must equal(Right(SubmissionRequestHeaders(LocalReferenceNumber(declarantLrnValue), Some(declarantDucrValue))))
      }

      "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
        val result: Either[ErrorResponse, SubmissionRequestHeaders] =
          validator.validateAndExtractSubmissionHeaders(Map.empty)

        result must equal(Left(ErrorResponse.errorInvalidHeaders))
      }

    }

    "validateCancellationHeaders" should {

      "return Right of validatedHeaderResponse when validateHeaders is called on valid headers" in new SetUp {
        val result: Either[ErrorResponse, CancellationRequestHeaders] =
          validator.validateAndExtractCancellationHeaders(ValidHeaders)
        result must equal(Right(CancellationRequestHeaders(Mrn(declarantMrnValue))))
      }

      "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
        val result: Either[ErrorResponse, CancellationRequestHeaders] =
          validator.validateAndExtractCancellationHeaders(Map.empty)
        result must equal(Left(ErrorResponse.errorInvalidHeaders))
      }

    }

    "validateAndExtractSubmissionNotificationHeaders" should {

      "return Right of SubmissionNotificationApiRequest when validateHeaders is called on valid headers" in new SetUp {
        val result: Either[ErrorResponse, NotificationApiRequestHeaders] =
          validator.validateAndExtractNotificationHeaders(ValidHeaders)
        result must equal(Right(NotificationApiRequestHeaders(AuthToken(dummyToken), ConversationId(actionId))))
      }

      "return Left ErrorResponse when validateHeaders is called with invalid headers" in new SetUp {
        val result: Either[ErrorResponse, NotificationApiRequestHeaders] =
          validator.validateAndExtractNotificationHeaders(Map.empty)
        result must equal(Left(ErrorResponse.errorInvalidHeaders))
      }

    }
  }
}
