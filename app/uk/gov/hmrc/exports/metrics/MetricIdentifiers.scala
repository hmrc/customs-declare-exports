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

package uk.gov.hmrc.exports.metrics

private[metrics] object MetricIdentifiers {
  val notificationMetric = "notification"

  val upstreamCustomsDeclarationsMetric = "upstream.customs-declarations"

  val declarationFindAllMetric = "declaration.findAll"
  val declarationFindSingleMetric = "declaration.findSingle"
  val declarationUpdateMetric = "declaration.update"

  val submissionMetric = "submission.declaration"
  val submissionProduceMetaDataMetric = "submission.declaration.produceMetaData"
  val submissionConvertToXmlMetric = "submission.declaration.convertToXml"
  val submissionUpdateDeclarationMetric = "submission.declaration.updateDeclarationStatus"
  val submissionFindOrCreateSubmissionMetric = "submission.declaration.findOrCreateSubmission"
  val submissionSendToDecApiMetric = "submission.declaration.sendToDecApi"
  val submissionAddSubmissionActionMetric = "submission.declaration.addSubmissionAction"
  val submissionAppendMrnToSubmissionMetric = "submission.declaration.appendMrnToSubmission"
}
