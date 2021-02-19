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

package uk.gov.hmrc.exports.repositories

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import reactivemongo.api.commands.WriteResult

trait RepoHelper {

  def handleWriteResult[T](writeResult: Future[WriteResult], document: T)(implicit ec: ExecutionContext): WriteResponse[T] =
    writeResult.transformWith {
      case Success(wr)  => Future(if (wr.ok) Right(document) else Left(resolveWriteResultError(wr)))
      case Failure(exc) => Future(Left(exc.getMessage))
    }

  private def resolveWriteResultError(writeResult: WriteResult): String =
    WriteResult
      .lastError(writeResult)
      .flatMap(_.errmsg.map(identity))
      .getOrElse("Unexpected error while inserting a document")
}
