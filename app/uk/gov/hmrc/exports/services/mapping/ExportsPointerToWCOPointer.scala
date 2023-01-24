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

import java.io.IOException
import scala.io.Source

object ExportsPointerToWCOPointer {

  val pointerFile = "conf/exports-wco-mapping.csv"

  // Negative look-ahead. Line must not start with "declaration." as it's added while building the mapping.
  private val regex1 = "^(?!declaration\\.).+".r

  private val mapping: Map[String, Seq[String]] = {
    val allLines = Source.fromFile(pointerFile).getLines().toList
    val lines = allLines.filter(line => line.count(_ == '|') == 1 && regex1.matches(line))
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
    s"declaration.$exportsPointer".replace("$", "") -> wcoPointer.replace("$", "")

  def getWCOPointers(exportsPointer: String): Seq[String] =
    mapping.get(exportsPointer) match {

      // Found. We just return the matching WCO Pointer.
      case Some(wcoPointers) => wcoPointers

      case _ =>
        // OK, not found. Let's see if it's a dynamic pointer (when contains at least one numeric-only segment).
        val segments = exportsPointer.split("\\.")
        if (segments.filter(_.toIntOption.nonEmpty).size == 0) List.empty // Nope. Nothing to do
        else getAsDynamicPointer(segments)
    }

  private def getAsDynamicPointer(segments: Array[String]): Seq[String] = {
    // Here we want to transform the dynamic exportsPointer in a key we could have in our mapping.
    // e.g. if exportsPointer is "declaration.items.5.additionalFiscalReferencesData.references.6.country"
    // we want to change it to "declaration.items.1.additionalFiscalReferencesData.references.2.country"

    val resultingExportsPointer = segments
      .foldLeft(0 -> "") { (tuple: (Int, String), s: String) =>
        if (s == "declaration") (0, s)
        else if (s.toIntOption.nonEmpty) (tuple._1 + 1, s"${tuple._2}.${tuple._1 + 1}")
        else (tuple._1, s"${tuple._2}.$s")
      }
      ._2

    mapping.get(resultingExportsPointer).fold(Seq.empty[String]) { // Not found.
      _.map(replacePlaceholders(_, segments.filter(_.toIntOption.nonEmpty))) // Found.
    }
  }

  private def replacePlaceholders(wcoPointer: String, placeholders: Array[String]): String =
    // Found. Now we need to replace in all wcoPointers, in the same position,
    // 1, 2, ... with the corresponding value in the source Exports Pointer.
    wcoPointer
      .split("\\.")
      .foldLeft(0 -> "") { (tuple: (Int, String), s: String) =>
        val index = tuple._1
        val result = tuple._2
        if (s.toIntOption.nonEmpty) (index + 1, s"$result.${placeholders(index)}")
        else (index, if (result.isEmpty) s else s"$result.$s")
      }
      ._2
}
