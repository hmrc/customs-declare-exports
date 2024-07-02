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

import com.sun.xml.bind.marshaller.CharacterEscapeHandler

import java.io.Writer
import java.lang.Character.{isISOControl, isLetterOrDigit, isWhitespace}
import scala.annotation.switch

class Sanitiser extends CharacterEscapeHandler {

  /* The default JAXB's handler (MinimumEscapeHandler) for escaping non-XML characters does not
     take into account most ISO control characters apart from \r and \n. On the contrary raises
     an exception, which we do not want if we can avoid it just removing them - the user in fact
     could unintentionally paste one of these control characters from an external source.

     Accordingly, any potential control character, apart from \n, is removed from a declaration's
     value during the conversion process to XML.
   */
  // scalastyle:off cyclomatic.complexity
  override def escape(array: Array[Char], from: Int, length: Int, isAttVal: Boolean, writer: Writer): Unit = {
    val sb = new StringBuilder(length)
    for (ix <- from until length) {
      val chr = array(ix)
      (chr: @switch) match {
        case ' '  => sb += ' ' // Avoid isWhitespace(' ') returning 'true'
        case '&'  => sb ++= "&amp;"
        case '<'  => sb ++= "&lt;"
        case '>'  => sb ++= "&gt;"
        case '\"' => if (isAttVal) sb ++= "&quot;" else sb += '\"'
        case '\n' => if (isAttVal) sb ++= "&#10;" else if (length == 1) sb += '\n'
        case _ =>
          if (!(isISOControl(chr) || isWhitespace(chr)))
            if (chr < 127 || isLetterOrDigit(chr)) sb += chr
            else sb ++= s"&#${chr.toInt};"
      }
    }

    writer.write(sb.toCharArray)
  }
}

object Sanitiser {

  /* For the same reason described above, that is the user unintentionally pasting a control
     character from an external source, we also need to remove these characters from a value
     of the declaration before comparing it with an expected string/value.
     An example of using the 'escape' method can be found in =>
       AdditionalInformationBuilder.buildThenAdd()
   */
  def escape(value: String): String = {
    val sb = new StringBuilder()
    value.foreach(chr => if (!isISOControl(chr)) sb += chr)
    sb.toString
  }
}
