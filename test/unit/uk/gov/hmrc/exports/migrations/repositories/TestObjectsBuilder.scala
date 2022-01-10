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

import com.mongodb.client.MongoCursor
import com.mongodb.{ServerAddress, ServerCursor}
import org.bson.Document

object TestObjectsBuilder {

  /**
    * Creates a dummy MongoCursor for testing purposes.
    *
    * Some of the methods are missing implementation, because they are not used in tests yet.
    * Only the iterative part of the MongoCursor is being used for now.
    *
    * @param elements sequence of Document objects to be iterated over
    * @return MongoCursor
    */
  def buildMongoCursor(elements: Seq[Document]): MongoCursor[Document] = new MongoCursor[Document] {
    private val iterator = elements.iterator

    override def close(): Unit = ???
    override def hasNext: Boolean = iterator.hasNext
    override def next(): Document = iterator.next
    override def tryNext(): Document = ???
    override def getServerCursor: ServerCursor = ???
    override def getServerAddress: ServerAddress = ???
  }
}
