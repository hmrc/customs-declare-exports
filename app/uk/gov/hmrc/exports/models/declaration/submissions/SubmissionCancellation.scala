package uk.gov.hmrc.exports.models.declaration.submissions

import play.api.libs.json.Json

case class SubmissionCancellation(
  functionalReferenceId: String,
  mrn: String,
  statementDescription: String,
  changeReason: String
)

object SubmissionCancellation {
  implicit val format = Json.format[SubmissionCancellation]
}
