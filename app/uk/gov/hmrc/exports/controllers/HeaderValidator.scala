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

import javax.inject.Singleton
import uk.gov.hmrc.exports.models._
@Singleton
class HeaderValidator {

  def extractLrnHeader(headers: Map[String, String]): Option[String] = {
    headers.get(CustomsHeaderNames.XLrnHeaderName)
  }

  def extractDucrHeader(headers: Map[String, String]): Option[String] = {
    headers.get(CustomsHeaderNames.XDucrHeaderName)
  }

  def extractMrnHeader(headers: Map[String, String]): Option[String] = {
    headers.get(CustomsHeaderNames.XMrnHeaderName)
  }


  def validateAndExtractSubmissionHeaders(implicit headers: Map[String, String])
    : Either[ErrorResponse, ValidatedHeadersSubmissionRequest] = {
    val result = for {
      lrn <- extractLrnHeader(headers)
      ducr <- extractDucrHeader(headers)
    } yield
      ValidatedHeadersSubmissionRequest(LocalReferenceNumber(lrn), Ducr(ducr))
    result match {
      case Some(vhr) =>
        Right(vhr)
      case _ =>
        Left(ErrorResponse.ErrorInternalServerError)
    }
  }

  def validateAndExtractCancellationHeaders(implicit headers: Map[String, String])
  : Either[ErrorResponse, ValidatedHeadersCancellationRequest] = {
    val result = for {
      mrn <- extractMrnHeader(headers)
    } yield
      ValidatedHeadersCancellationRequest(Mrn(mrn))
    result match {
      case Some(vhr) =>
        Right(vhr)
      case _ =>
        Left(ErrorResponse.ErrorInternalServerError)
    }
  }
}
