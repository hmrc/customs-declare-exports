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

package uk.gov.hmrc.exports.mongock

import com.github.cloudyrock.mongock.MongockBuilder
import com.google.inject.Singleton
import com.mongodb.{MongoClient, MongoClientURI}

@Singleton
case class MongockConfig(mongoURI: String) {

  val uri = new MongoClientURI(mongoURI.replaceAllLiterally("sslEnabled", "ssl"))

  val client = new MongoClient(uri)

  val runner = new MongockBuilder(client, uri.getDatabase, "uk.gov.hmrc.exports.mongock.changesets")
    .build()

  runner.execute()
  runner.close()
}
