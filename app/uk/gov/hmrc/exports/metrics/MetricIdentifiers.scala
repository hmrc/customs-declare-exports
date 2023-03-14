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

package uk.gov.hmrc.exports.metrics

private[metrics] object MetricIdentifiers {

  object NotificationMetric {
    val notificationReceiptHandling = "notification"
  }

  object UpstreamMetric {
    val customsDeclarations = "upstream.customs-declarations"
  }

  object DeclarationMetric {
    val findAll = "declaration.findAll"
    val findSingle = "declaration.findSingle"
    val update = "declaration.update"
  }

  object SubmissionMetric {
    val wholeSubmission = "submission.declaration"
    val produceMetaData = "submission.declaration.produceMetaData"
    val convertToXml = "submission.declaration.convertToXml"
    val updateDeclaration = "submission.declaration.updateDeclarationStatus"
    val findOrCreateSubmission = "submission.declaration.findOrCreateSubmission"
    val sendToDecApi = "submission.declaration.sendToDecApi"
    val addSubmissionAction = "submission.declaration.addSubmissionAction"
    val appendMrnToSubmission = "submission.declaration.appendMrnToSubmission"
  }

  object AmendmentMetric {
    val wholeSubmission = "amendment.declaration"
    val produceMetaData = "amendment.declaration.produceMetaData"
    val convertToXml = "amendment.declaration.convertToXml"
    val updateDeclaration = "amendment.declaration.updateDeclarationStatus"
    val sendToDecApi = "amendment.declaration.sendToDecApi"
    val addSubmissionAction = "amendment.declaration.addSubmissionAction"
  }
}
