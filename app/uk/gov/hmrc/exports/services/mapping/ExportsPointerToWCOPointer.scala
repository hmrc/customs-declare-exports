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

package uk.gov.hmrc.exports.services.mapping

import play.api.Environment

import java.io.IOException
import javax.inject.{Inject, Singleton}
import scala.io.Source

@Singleton
class ExportsPointerToWCOPointer @Inject() (environment: Environment) {

  private val pointerFile = "exports-wco-mapping.csv"

  // Negative look-ahead. Line must not start with "declaration." as it's added while building the mapping.
  private val regex = "^(?!declaration\\.).+".r

  protected[this] val mapping: Map[String, Seq[String]] = {
    val stream = environment.resourceAsStream(pointerFile).getOrElse(throw new Exception(s"$pointerFile could not be read!"))
    val allLines = Source.fromInputStream(stream).getLines().toList
    stream.close()
    val lines = allLines.filter(line => line.count(_ == '|') == 1 && regex.matches(line))
    if (lines.size != allLines.size)
      throw new IOException(s"File $pointerFile is malformed. Expecting rows NOT starting with 'declaration.' and with 2 values separated by '|'")

    val tuples = lines.map(_.split('|').map(_.trim).toList) collect {
      case List(exportsPointer, wcoPointer) if exportsPointer.nonEmpty && wcoPointer.nonEmpty =>
        if (exportsPointer.count(_ == '$') == wcoPointer.count(_ == '$')) removeDollarSign(exportsPointer, wcoPointer)
        else throw new IOException(s"File $pointerFile is malformed. Not matching '$$' for $exportsPointer")

      case line =>
        throw new IOException(s"File $pointerFile is malformed. Empty value(s) in line ($line)")
    }

    // Group by Exports Pointer. Result is (Exports Pointer -> List of WCO Pointers) (1 to many)
    tuples.groupMap(_._1)(_._2)
  }

  private def removeDollarSign(exportsPointer: String, wcoPointer: String): (String, String) =
    s"declaration.$exportsPointer".replace("$", "") -> wcoPointer

  def getWCOPointers(exportsPointer: String): Either[MappingError, Seq[String]] = {
    val segments = exportsPointer.split("\\.")
    val placeholders = segments.filter(isSequenceNumber)

    mapping.get(exportsPointer) match {

      // List of WCO Pointers found.
      case Some(wcoPointers) =>
        Right(wcoPointers.map { wcoPointer =>
          // Replacing placeholders, if any, in the WCO Pointers.
          if (wcoPointer.contains('$')) replacePlaceholders(wcoPointer, placeholders)
          else wcoPointer
        })

      // OK, not found. Let's see if it's a dynamic pointer (when contains at least one numeric-only segment).
      case _ =>
        val dynamicPointers = getAsDynamicPointer(segments)
        if (dynamicPointers.isEmpty) Left(NoMappingFoundError(exportsPointer))
        else Right(dynamicPointers)
    }
  }

  private def getAsDynamicPointer(segments: Array[String]): Seq[String] = {
    // Here we want to transform the dynamic exportsPointer in a key we could have in our mapping.
    // e.g. if exportsPointer is "declaration.items.5.additionalFiscalReferencesData.references.6.country"
    // we want to change it to "declaration.items.1.additionalFiscalReferencesData.references.2.country"

    val resultingExportsPointer = segments
      .foldLeft(0 -> "") { (tuple: (Int, String), s: String) =>
        if (s == "declaration") (0, s)
        else if (isSequenceNumber(s)) (tuple._1 + 1, s"${tuple._2}.${tuple._1 + 1}")
        else (tuple._1, s"${tuple._2}.$s")
      }
      ._2

    mapping.get(resultingExportsPointer).fold(Seq.empty[String]) { // Not found.
      val placeholders = segments.filter(isSequenceNumber) // Found.
      _.map(replacePlaceholders(_, placeholders)) // Found.
    }
  }

  private val regex2 = "^\\$[0-9]+".r

  private def replacePlaceholders(wcoPointer: String, placeholders: Array[String]): String =
    // Found. Now we need to replace in all wcoPointers, in the same position,
    // 1, 2, ... with the corresponding value in the source Exports Pointer.
    wcoPointer
      .split("\\.")
      .foldLeft(0 -> "") { (tuple: (Int, String), s: String) =>
        val index = tuple._1
        val result = tuple._2
        if (regex2.matches(s)) (index + 1, s"$result.${placeholders(index)}")
        else (index, if (result.isEmpty) s else s"$result.$s")
      }
      ._2

  private def isSequenceNumber(segment: String): Boolean = segment.startsWith("#") && segment.drop(1).toIntOption.nonEmpty
}
