/*
 * Copyright 2024 HM Revenue & Customs
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

import org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler

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
  override def escape(array: Array[Char], from: Int, length: Int, isAttVal: Boolean, writer: Writer): Unit =
    if (length == 1) escapeSingleChar(array(from), isAttVal, writer)
    else {
      val sb = new StringBuilder(length)
      for (ix <- from until length) {
        val chr = array(ix)
        (chr: @switch) match {

          // Might be used to separate 2 words
          case '\t' => sb += ' '

          case '\n' => if (isAttVal) sb ++= "&#10;" else sb += ' '

          // Replaced with a space only if the next char is not a line-feed.
          case '\r' => if ((ix + 1) < length && array(ix + 1) != '\n') sb += ' '

          case '\"' => if (isAttVal) sb ++= "&quot;" else sb += '\"'
          case '&'  => sb ++= "&amp;"
          case '<'  => sb ++= "&lt;"
          case '>'  => sb ++= "&gt;"
          case _    =>
            if ((chr > 31 && chr < 127) || isLetterOrDigit(chr)) sb += chr
            else // Whitespace and control chars are ignored. Any other char is escaped.
            if (!(isWhitespace(chr) || isISOControl(chr))) sb ++= s"&#${chr.toInt};"
        }
      }

      writer.write(sb.toCharArray)
    }
  // scalastyle:on

  /* In order to improve the performance of the conversion process to XML, we handles values
     consisting of a single character, and we have several of them in a declaration, by using
     an ad-hoc method.

     Whereas for values with len > 1 most of the whitespace and control chars are just ignored,
     when they are the unique char of a value escapeSingleChar simply replaces them with a ' ',
     let aside line-feed chars, so to not write out an empty value.

     In addition to avoiding the creation of an unnecessary StringBuilder instances, we can also
     take advantage of the improved performance of 'writer.write(Char)' over 'writer.write(String)'.
   */
  private def escapeSingleChar(chr: Char, isAttVal: Boolean, writer: Writer): Unit =
    (chr: @switch) match {
      case '\n' => if (isAttVal) writer.write("&#10;") else writer.write('\n')
      case '\"' => if (isAttVal) writer.write("&quot;") else writer.write('\"')
      case '&'  => writer.write("&amp;")
      case '<'  => writer.write("&lt;")
      case '>'  => writer.write("&gt;")
      case _    =>
        if ((chr > 31 && chr < 127) || isLetterOrDigit(chr)) writer.write(chr.toInt)
        else if (isWhitespace(chr) || isISOControl(chr)) writer.write(' ')
        else writer.write(s"&#${chr.toInt};")
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
