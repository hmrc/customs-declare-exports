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

package repositories

import com.mongodb.ErrorCategory.DUPLICATE_KEY
import com.mongodb.ExplainVerbosity.QUERY_PLANNER
import com.mongodb.client.model.{ReturnDocument, Updates}
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions, InsertOneModel}
import org.mongodb.scala.{ClientSession, Document, MongoCollection, MongoWriteException}
import play.api.libs.json.JsValue
import play.libs.Json

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

// scalastyle:off
trait RepositoryOps[T] {

  implicit def classTag: ClassTag[T]
  implicit val executionContext: ExecutionContext

  val collection: MongoCollection[T]

  def bulkInsert(documents: Seq[T]): Future[Int] =
    collection.bulkWrite(documents.map(InsertOneModel(_))).toFuture().map(_.getInsertedCount)

  def bulkInsert(session: ClientSession, documents: Seq[T]): Future[Int] =
    collection.bulkWrite(session, documents.map(InsertOneModel(_))).toFuture().map(_.getInsertedCount)

  def create(document: T): Future[T] =
    collection.insertOne(document).toFuture().map(_ => document)

  def explain(filter: Bson): Future[String] =
    collection
      .find(filter)
      .explain(QUERY_PLANNER)
      .toFuture()
      .map {
        _.toList.map { t =>
          if (t._1 != "operationTime") s"\"${t._1}\" : ${t._2}"
          else t._2.toString.replace("Timestamp", "\"Timestamp\" : ")
        }.mkString("{", ",", "}")
      }

  def findAll(): Future[Seq[T]] =
    collection.find().toFuture()

  def findAll[V](keyId: String, keyValue: V): Future[Seq[T]] =
    collection.find(equal(keyId, keyValue)).toFuture()

  def findAll(filter: JsValue): Future[Seq[T]] =
    collection.find(BsonDocument(filter.toString)).toFuture()

  def findAll(filter: Bson): Future[Seq[T]] =
    collection.find(filter).toFuture()

  def findAll(clientSession: ClientSession, filter: Bson): Future[Seq[T]] =
    collection.find(clientSession, filter).toFuture()

  def findFirst(filter: JsValue, sort: JsValue): Future[Option[T]] =
    findFirst(BsonDocument(filter.toString), BsonDocument(sort.toString))

  def findFirst(filter: Bson, sort: Bson): Future[Option[T]] =
    collection.find(filter).sort(sort).limit(1).toFuture().map(_.headOption)

  def findOne[V](keyId: String, keyValue: V): Future[Option[T]] =
    findOne(equal(keyId, keyValue))

  def findOne(filter: JsValue): Future[Option[T]] =
    findOne(BsonDocument(filter.toString))

  def findOne(filter: Bson): Future[Option[T]] =
    collection.find(filter).limit(1).toFuture().map(_.headOption)

  /*
   Find one and return if a document with keyId=keyValue exists,
   or create "document: T" if a document with keyId=keyValue does NOT exists.
   */
  def findOneOrCreate[V](keyId: String, keyValue: V, document: => T): Future[T] =
    findOneOrCreate(equal(keyId, keyValue), document)

  /*
   Find one and return if a document with the given filter exists,
   or create "document: T" if a document with the given filter does NOT exists.
   */
  def findOneOrCreate(filter: JsValue, document: => T): Future[T] =
    findOneOrCreate(BsonDocument(filter.toString), document)

  private lazy val upsertAndReturnAfter = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)

  def findOneOrCreate(filter: Bson, document: => T): Future[T] =
    collection
      .findOneAndUpdate(filter = filter, update = Updates.setOnInsert(BsonDocument(Json.toJson(document).toString)), options = upsertAndReturnAfter)
      .toFuture()

  def findOneAndRemove[V](keyId: String, keyValue: V): Future[Option[T]] =
    collection.findOneAndDelete(equal(keyId, keyValue)).toFutureOption()

  def findOneAndRemove(filter: JsValue): Future[Option[T]] =
    collection.findOneAndDelete(BsonDocument(filter.toString)).toFutureOption()

  def findOneAndRemove(filter: Bson): Future[Option[T]] =
    collection.findOneAndDelete(filter).toFutureOption()

  /*
   Find one and replace with "document: T" if a document with keyId=keyValue exists,
   or create "document: T" (if createIfNotExists is true) if a document with keyId=keyValue does NOT exists.
   */
  def findOneAndReplace[V](keyId: String, keyValue: V, document: => T, createIfNotExists: Boolean): Future[Option[T]] =
    findOneAndReplace(equal(keyId, keyValue), document, createIfNotExists)

  /*
   Find one and replace if a document with the given filter exists,
   or create "document: T" (if createIfNotExists is true) if a document with the given filter does NOT exists.
   */
  def findOneAndReplace(filter: JsValue, document: => T, createIfNotExists: Boolean): Future[Option[T]] =
    findOneAndReplace(BsonDocument(filter.toString), document, createIfNotExists)

  def findOneAndReplace(filter: Bson, document: => T, createIfNotExists: Boolean = true): Future[Option[T]] = {
    val result = collection
      .findOneAndReplace(
        filter = filter,
        replacement = document,
        options = FindOneAndReplaceOptions().upsert(createIfNotExists).returnDocument(ReturnDocument.AFTER)
      )

    if (result == null) Future.successful(None) else result.toFutureOption()
  }

  /*
   Find and update a document if a document with keyId = keyValue exists. Do not create a new document
   */
  def findOneAndUpdate[V](keyId: String, keyValue: V, update: JsValue): Future[Option[T]] =
    findOneAndUpdate(equal(keyId, keyValue), BsonDocument(update.toString))

  /*
   Find and update a document if a document with the given filter exists. Do not create a new document
   */
  def findOneAndUpdate(filter: JsValue, update: JsValue): Future[Option[T]] =
    findOneAndUpdate(BsonDocument(filter.toString), BsonDocument(update.toString))

  protected def doNotUpsertAndReturnAfter = FindOneAndUpdateOptions().upsert(false).returnDocument(ReturnDocument.AFTER)

  def findOneAndUpdate(filter: Bson, update: Bson): Future[Option[T]] =
    collection
      .findOneAndUpdate(filter = filter, update = update, options = doNotUpsertAndReturnAfter)
      .toFutureOption()

  def findOneAndUpdate(session: ClientSession, filter: Bson, update: Bson): Future[Option[T]] =
    collection
      .findOneAndUpdate(session, filter = filter, update = update, options = doNotUpsertAndReturnAfter)
      .toFutureOption()

  def get[V](keyId: String, keyValue: V): Future[T] =
    get(equal(keyId, keyValue))

  def get(filter: JsValue): Future[T] =
    get(BsonDocument(filter.toString))

  def get(filter: Bson): Future[T] =
    collection.find(filter).limit(1).toFuture().map(_.head)

  def indexList: Future[Seq[Document]] = collection.listIndexes().toFuture()

  def insertOne(document: T): Future[Either[WriteError, T]] =
    collection
      .insertOne(document)
      .toFuture()
      .map(_ => Right(document))
      .recover {
        case exc: MongoWriteException if exc.getError.getCategory == DUPLICATE_KEY =>
          Left(DuplicateKey(exc.getError.getMessage))
      }

  def removeAll: Future[Long] =
    collection.deleteMany(BsonDocument()).toFuture().map(_.getDeletedCount)

  def removeEvery[V](keyId: String, keyValue: V): Future[Long] =
    collection.deleteMany(equal(keyId, keyValue)).toFuture().map(_.getDeletedCount)

  def removeEvery(filter: JsValue): Future[Long] =
    collection.deleteMany(BsonDocument(filter.toString)).toFuture().map(_.getDeletedCount)

  def removeEvery(filter: Bson): Future[Long] =
    collection.deleteMany(filter).toFuture().map(_.getDeletedCount)

  def removeEvery(clientSession: ClientSession, filter: Bson): Future[Long] =
    collection.deleteMany(clientSession, filter).toFuture().map(_.getDeletedCount)

  def removeOne[V](keyId: String, keyValue: V): Future[Boolean] =
    collection.deleteOne(equal(keyId, keyValue)).toFuture().map(_.getDeletedCount > 0)

  def removeOne(filter: JsValue): Future[Boolean] =
    collection.deleteOne(BsonDocument(filter.toString)).toFuture().map(_.getDeletedCount > 0)

  def removeOne(filter: Bson): Future[Boolean] =
    collection.deleteOne(filter).toFuture().map(_.getDeletedCount > 0)

  def size: Future[Long] = collection.countDocuments().toFuture()
}
// scalastyle:on

sealed abstract class WriteError(message: String)

case class DuplicateKey(message: String) extends WriteError(message)
