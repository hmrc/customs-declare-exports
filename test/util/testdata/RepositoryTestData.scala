/*
 * Copyright 2021 HM Revenue & Customs
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

package testdata

import scala.concurrent.Future
import scala.util.control.NoStackTrace

import reactivemongo.api.commands.{LastError, WriteResult}
import uk.gov.hmrc.exports.repositories.WriteResponse
object RepositoryTestData {

  def dummyWriteResponseFailure[T]: WriteResponse[T] = Future.successful(Left("Cannot insert document"))
  def dummyWriteResponseSuccess[T](document: T): WriteResponse[T] = Future.successful(Right(document))

  val dummyWriteResultSuccess: Future[WriteResult] =
    Future.successful(LastError(true, None, None, None, 0, None, false, None, None, false, None, None))

  val dummyWriteResultFailure: Future[WriteResult] =
    Future.failed[WriteResult](new RuntimeException("Test Exception message") with NoStackTrace)
}
