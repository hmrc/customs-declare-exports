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

package uk.gov.hmrc.exports.services.audit

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.services.audit.AuditTypes.NotificationProcessed

object AuditNotifications {
  def audit(
    submission: Submission,
    declarationId: String,
    functionCodes: Seq[String],
    validationCodes: Seq[String],
    conversationId: String,
    auditService: AuditService
  ): Unit = {
    val auditData: Map[String, String] = Map(
      EventData.ducr.toString -> submission.ducr,
      EventData.lrn.toString -> submission.lrn,
      EventData.eori.toString -> submission.eori,
      EventData.declarationId.toString -> submission.uuid,
      EventData.conversationId.toString -> conversationId
    )

    val optionalKeyValues = Seq((EventData.mrn.toString -> submission.mrn))

    val optionalDataElements = optionalKeyValues.foldLeft(Map.empty[String, String]) { (acc, item) =>
      item._2
        .map(v => acc + (item._1 -> v))
        .getOrElse(acc)
    }

    def mergeValidationCodes(payload: JsObject): JsObject =
      if (validationCodes.isEmpty) payload
      else {
        val arrayField = Json.toJson(Map(EventData.validationCodes.toString -> validationCodes)).as[JsObject]
        payload.deepMerge(arrayField)
      }

    def mergeFunctionCodes(payload: JsObject): JsObject =
      if (functionCodes.isEmpty) payload
      else {
        val arrayField = Json.toJson(Map(EventData.functionCodes.toString -> functionCodes)).as[JsObject]
        payload.deepMerge(arrayField)
      }

    val mergeArrayValues = mergeValidationCodes _ andThen mergeFunctionCodes _

    val auditPayload = Json.toJson(auditData ++ optionalDataElements).as[JsObject]
    val completePayload = mergeArrayValues(auditPayload)

    auditService.auditNotificationProcessed(NotificationProcessed, completePayload)
  }
}
