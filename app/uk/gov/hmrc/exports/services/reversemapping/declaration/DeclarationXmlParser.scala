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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import scala.annotation.tailrec

object DeclarationXmlParser {

  type XmlParserResult[A] = Either[XmlParsingException, A]

  implicit class EitherList[T](list: Seq[XmlParserResult[T]]) {

    def toEitherOfList: XmlParserResult[Seq[T]] = {
      @tailrec
      def insideOut(rest: Seq[XmlParserResult[T]], acc: Seq[T]): XmlParserResult[Seq[T]] = rest match {
        case Nil                => Right(acc)
        case Right(value) :: tl => insideOut(tl, acc :+ value)
        case Left(e) :: _       => Left(e)
      }

      insideOut(list, Nil)
    }
  }
}
