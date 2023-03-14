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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.Amendment
import wco.datamodel.wco.declaration_ds.dms._2.{PointerDocumentSectionCodeType, PointerTagIDType}

import javax.inject.Inject

class AmendmentPointerBuilder @Inject() () extends ModifyingBuilder[String, Amendment] {

  override def buildThenAdd(wcoPointerString: String, amendment: Amendment): Unit = {

    val pointerTuples = parseWCOPointer(wcoPointerString)

    pointerTuples.foreach { case (sectionCode, sequenceNbr, tagId) =>
      val pointer = new Amendment.Pointer()

      val pointerSection = new PointerDocumentSectionCodeType()
      pointerSection.setValue(sectionCode)
      pointer.setDocumentSectionCode(pointerSection)

      sequenceNbr.foreach { nbr =>
        pointer.setSequenceNumeric(new java.math.BigDecimal(nbr))
      }

      tagId.foreach { tagId =>
        val pointerTagIDType = new PointerTagIDType()
        pointerTagIDType.setValue(tagId)
        pointer.setTagID(pointerTagIDType)
      }

      amendment.getPointer().add(pointer)
    }
  }

  private def parseWCOPointer(wcoPointerString: String): Seq[(String, Option[Int], Option[String])] = {

    @annotation.tailrec
    def parse(result: Seq[(String, Option[Int], Option[String])], remaining: Seq[String]): Seq[(String, Option[Int], Option[String])] =
      if (remaining.size <= 0) result
      else {
        val item = remaining.head
        val newResult = if (item.last.isLetter) { // item is a sectionCode
          result :+ (item, None, None)
        } else {
          val lastResult = result.last
          item.toIntOption match {
            case Some(intVal) if remaining.size != 1 => // item is a sequenceNbr
              result.init :+ (lastResult._1, Some(intVal), lastResult._3)
            case _ => // item is a tagId
              result.init :+ (lastResult._1, lastResult._2, Some(item))
          }
        }

        parse(newResult, remaining.tail)
      }

    val parts = wcoPointerString.split("\\.").toSeq
    parse(Seq.empty[(String, Option[Int], Option[String])], parts)
  }
}
