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

import com.google.inject.Inject
import play.api.Logging
import play.api.libs.json.JsObject
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.services.audit.AuditTypes.Audit
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure, Success}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.audit.AuditExtensions

import scala.concurrent.ExecutionContext

class AuditService @Inject() (connector: AuditConnector, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  def auditNotificationProcessed(audit: Audit, auditData: JsObject) = {

    val extendedEvent = ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType = audit.toString,
      tags = getAuditTags(s"${audit.toString}", path = s"${audit.toString}")(HeaderCarrier()),
      detail = auditData
    )

    connector.sendExtendedEvent(extendedEvent).map(handleResponse(_, audit.toString))
  }

  private def handleResponse(result: AuditResult, auditType: String): AuditResult = result match {
    case Success =>
      logger.debug(s"Exports $auditType audit successful")
      Success

    case Failure(err, _) =>
      logger.warn(s"Exports $auditType Audit Error, message: $err")
      Failure(err)

    case Disabled =>
      logger.warn(s"Auditing Disabled")
      Disabled
  }

  private def getAuditTags(transactionName: String, path: String)(implicit hc: HeaderCarrier): Map[String, String] =
    AuditExtensions
      .auditHeaderCarrier(hc)
      .toAuditTags(transactionName = s"export-declaration-${transactionName.toLowerCase}", path = s"customs-declare-exports/$path")

}

object AuditTypes extends Enumeration {
  type Audit = Value
  val NotificationProcessed = Value
}

object EventData extends Enumeration {
  type Data = Value

  val eori, lrn, mrn, ducr, decType, declarationId, conversationId, functionCodes, validationCodes = Value
}
