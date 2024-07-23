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
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class ExportsPointerToWCOPointer @Inject() (environment: Environment) {

  private val pointerFile = "code-lists/exports-wco-mapping.json"

  protected[this] def mapping(implicit reader: Reads[Pointers]): Map[String, Seq[String]] = {

    val stream = environment.resourceAsStream(pointerFile).getOrElse(throw new Exception(s"$pointerFile could not be read!"))

    Try(Json.parse(stream)) match {
      case Success(JsArray(jsValues)) =>
        val items: Seq[JsResult[Pointers]] = jsValues.toList.map { jsValue =>
          reader.reads(jsValue)
        }

        items
          .flatMap(_.asOpt)
          .groupMap(_.cds)(_.wco)

      case Success(_)  => throw new IllegalArgumentException(s"Could not read JSON array from file: '$pointerFile'")
      case Failure(ex) => throw new IllegalArgumentException(s"Failed to read JSON file: '$pointerFile'", ex)
    }

  }

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

private case class Pointers(cds: String, wco: String, flags: Option[Seq[(String, String)]], comments: Option[Seq[String]])

private object PointersReads extends ConstraintReads {

  private val regex = "^(?!declaration\\.).+".r

  val cdsReads: Reads[String] =
    filter(JsonValidationError("Expecting non empty rows NOT starting with 'declaration.'")) { cds =>
      regex.matches(cds) && cds.nonEmpty
    }

  val wcoReads: Reads[String] =
    filter(JsonValidationError("Empty WCO pointer"))(_.nonEmpty)

  val flagsReads: Reads[Seq[(String, String)]] = Reads[Seq[(String, String)]] { json =>
    json.validate[Seq[JsObject]].map { objects =>
      objects.flatMap(_.fields).map { case (key, value) =>
        (key, value.as[String])
      }
    }
  }

  // The following method is a temporary hack to handle pointers that come from the frontend as a sequence, but for reasons of expediency we are mapping to the WCO Pointer that is actually a parent of the sequenced elements.
  // Example: There can be up to 99 taric or nact codes, but we will point to the Commodity element if any of these change.
  // These are handled by removing the final sequence id so that this class will parse them.
  def convertManyToOnePointers(pointer: String): String =
    if (
      Seq(
        "items.$1.nactCode.$2",
        "items.$1.taricCode.$2",
        "items.$1.procedureCodes.additionalProcedureCodes.$2",
        "locations.routingCountries.$1"
      ) contains pointer
    ) pointer.dropRight(3)
    else pointer

}

private object Pointers {

  def apply(cds: String, wco: String, flags: Option[Seq[(String, String)]] = None, comments: Option[Seq[String]] = None): Pointers =
    new Pointers(s"declaration.$cds".replace("$", ""), wco, flags, comments)

  implicit val reads: Reads[Pointers] = new Reads[Pointers] {

    import PointersReads._

    override def reads(json: JsValue): JsResult[Pointers] = {

      val cds: String = (json \ "cds").as[String](cdsReads)
      val wco: String = (json \ "wco").as[String](wcoReads)

      val flags: Option[Seq[(String, String)]] = (json \ "flags").asOpt[Seq[(String, String)]](flagsReads)
      val comments: Option[Seq[String]] = (json \ "comments").asOpt[Seq[String]]

      if (convertManyToOnePointers(cds).count(_ == '$') == wco.count(_ == '$')) {
        JsSuccess(Pointers(cds, wco, flags, comments))
      } else throw JsResultException(Seq((__, Seq(JsonValidationError(s"Not matching '$$' for $cds -> $wco")))))

    }
  }

}
