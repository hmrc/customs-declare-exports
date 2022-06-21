/*
 * Copyright 2022 HM Revenue & Customs
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

import org.bson.Document
import uk.gov.hmrc.exports.base.UnitSpec

import java.time.Instant
import scala.collection.JavaConverters.mapAsJavaMap

class LockEntrySpec extends UnitSpec {

  "LockEntry on buildFullDBObject" should {
    "convert to correct Document" in {
      val date = Instant.now
      val expectedOutput =
        new Document(mapAsJavaMap(Map("key" -> "keyValue", "status" -> "statusValue", "owner" -> "ownerValue", "expiresAt" -> date.toString)))

      val lockEntry = LockEntry(key = "keyValue", status = "statusValue", owner = "ownerValue", expiresAt = date)
      lockEntry.buildFullDBObject mustBe expectedOutput
    }
  }
}
