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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import uk.gov.hmrc.exports.models.StringOption
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult

import java.lang.Character.isDigit
import scala.annotation.tailrec
import scala.xml.NodeSeq

trait DeclarationXmlParser[A] {
  def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[A]
}

object DeclarationXmlParser {

  type XmlParserError = String
  type XmlParserResult[A] = Either[XmlParserError, A]

  implicit class EitherList[T](list: Seq[XmlParserResult[T]]) {

    def toEitherOfList: XmlParserResult[Seq[T]] = {
      @tailrec
      def insideOut(rest: Seq[XmlParserResult[T]], acc: Seq[T]): XmlParserResult[Seq[T]] =
        (rest: @unchecked) match {
          case Nil                => Right(acc)
          case Right(value) :: tl => insideOut(tl, acc :+ value)
          case Left(e) :: _       => Left(e)
        }

      insideOut(list, Nil)
    }
  }

  implicit class OptionalString(nodeSeq: NodeSeq) {
    def toStringOption: Option[String] = StringOption(nodeSeq.text)
  }

  def parseIntIfAny(listOfNode: List[Option[String]], index: Int, xmlPath: => String): XmlParserResult[Option[Int]] = {
    def error(sequenceId: String): Left[XmlParserError, Option[Int]] = {
      val msg = s"Expected a numeric value for '$xmlPath' element"
      Left(s"$msg. The element was instead $sequenceId")
    }
    listOfNode
      .lift(index)
      .flatten
      .map { value =>
        if (value forall isDigit) Right(Some(value.toInt)) else error(value)
      }
      .getOrElse(Right(None))
  }

  def parseSequenceId(listOfNode: List[Option[String]], xmlPath: => String): XmlParserResult[Int] = {
    def error(sequenceId: String): Left[XmlParserError, Int] = {
      val msg = s"Expected a numeric value for '$xmlPath' element"
      Left(s"$msg. The element was instead $sequenceId")
    }
    listOfNode.headOption.flatten.map { sequenceId =>
      if (sequenceId forall isDigit) Right(sequenceId.toInt) else error(s"'$sequenceId'")
    }
      .getOrElse(error("missing/not found"))
  }

  def parseText(listOfNode: List[Option[String]], index: Int, xmlPath: => String): XmlParserResult[String] = {
    def error: Left[XmlParserError, String] =
      Left(s"Expected a value for '$xmlPath' element. The element was instead missing/not found")

    listOfNode.lift(index).flatten.map(Right(_)).getOrElse(error)
  }
}
