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

package uk.gov.hmrc.exports.services

import javax.inject.Inject
import play.api.Logger
import play.mvc.Http.Status.ACCEPTED
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.CustomsDeclarationsResponse
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class WcoSubmissionService @Inject()(
  wcoMapperService: WcoMapperService,
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository
) {
  private val logger = Logger(this.getClass)

  def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier, execution: ExecutionContext) = {

    val metaData = wcoMapperService.produceMetaData(declaration)
    val lrn = wcoMapperService.declarationLrn(metaData)
    val ducr = wcoMapperService.declarationUcr(metaData)

    customsDeclarationsConnector.submitDeclaration(declaration.eori, wcoMapperService.toXml(metaData)).flatMap {
      case CustomsDeclarationsResponse(ACCEPTED, Some(conversationId)) =>
        val newSubmission =
          Submission(
            eori = declaration.eori,
            lrn = lrn.getOrElse(throw new IllegalArgumentException("An LRN is required")),
            ducr = ducr,
            actions = Seq(Action(requestType = SubmissionRequest, conversationId = conversationId))
          )

        submissionRepository
          .save(newSubmission)
          .map(
            outcome =>
              notificationRepository.findNotificationsByConversationId(conversationId).flatMap { notifications =>
                val result = Future.successful(outcome)
                notifications.headOption.map(_.mrn).fold(result) { mrn =>
                  submissionRepository.updateMrn(conversationId, mrn).flatMap(_ => result)
                }
            }
          )

      case CustomsDeclarationsResponse(status, _) =>
        logger
          .error(s"Customs Declarations Service returned $status for Eori: $declaration.eori and lrn: $lrn")
        Future.successful(Left("Non Accepted status returned by Customs Declarations Service"))
    }
  }
}
