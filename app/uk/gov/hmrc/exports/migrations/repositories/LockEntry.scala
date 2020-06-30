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

package uk.gov.hmrc.exports.migrations.repositories

import java.util.Date

import org.bson.Document
import uk.gov.hmrc.exports.migrations.repositories.LockEntry._

object LockEntry {
  private[migrations] val KeyField: String = "key"
  private[migrations] val StatusField: String = "status"
  private[migrations] val OwnerField: String = "owner"
  private[migrations] val ExpiresAtField: String = "expiresAt"
}

case class LockEntry(key: String, status: String, owner: String, expiresAt: Date) {

  private[migrations] def buildFullDBObject: Document = {
    val entry: Document = new Document
    entry.append(KeyField, this.key).append(StatusField, this.status).append(OwnerField, this.owner).append(ExpiresAtField, this.expiresAt)
  }

  private[migrations] def isOwner(owner: String): Boolean = this.owner == owner

}
