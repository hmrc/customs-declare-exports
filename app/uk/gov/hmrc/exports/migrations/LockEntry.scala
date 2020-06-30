/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.migrations

import java.util.Date

import org.bson.Document
import uk.gov.hmrc.exports.migrations.LockEntry._

object LockEntry {
  private[migrations] val KEY_FIELD: String = "key"
  private[migrations] val STATUS_FIELD: String = "status"
  private[migrations] val OWNER_FIELD: String = "owner"
  private[migrations] val EXPIRES_AT_FIELD: String = "expiresAt"
}

case class LockEntry(key: String, status: String, owner: String, expiresAt: Date) {

  private[migrations] def buildFullDBObject: Document = {
    val entry: Document = new Document
    entry.append(KEY_FIELD, this.key).append(STATUS_FIELD, this.status).append(OWNER_FIELD, this.owner).append(EXPIRES_AT_FIELD, this.expiresAt)
  }

  private[migrations] def isOwner(owner: String): Boolean = this.owner == owner

}
