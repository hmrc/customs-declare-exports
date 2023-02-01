/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services

import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionAction}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WcoSubmissionService @Inject() (wcoMapperService: WcoMapperService, customsDeclarationsConnector: CustomsDeclarationsConnector) {

  def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier, execution: ExecutionContext): Future[Submission] = {
    val metaData = wcoMapperService.produceMetaData(declaration)
    val lrn = wcoMapperService
      .declarationLrn(metaData)
      .getOrElse(throw new IllegalArgumentException("An LRN is required"))
    val ducr = wcoMapperService
      .declarationDucr(metaData)
      .getOrElse(throw new IllegalArgumentException("An DUCR is required"))
    val payload = wcoMapperService.toXml(metaData)

    customsDeclarationsConnector.submitDeclaration(declaration.eori, payload) map { conversationId =>
      Submission(
        uuid = declaration.id,
        eori = declaration.eori,
        lrn = lrn,
        ducr = ducr,
        actions = Seq(SubmissionAction(id = conversationId)),
        latestDecId = declaration.id
      )
    }
  }
}
