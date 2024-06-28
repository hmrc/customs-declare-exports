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

import com.sun.xml.bind.marshaller.{CharacterEscapeHandler, MinimumEscapeHandler}

import java.io.Writer

class Sanitiser extends CharacterEscapeHandler {

  /* The default JAXB's handler for escaping non-XML characters does not take into account most
     ISO control characters apart from \r and \n. On the contrary raises an exception, which we
     do not want if we can avoid it just removing them - the user in fact, could unintentionally
     paste one of these control characters from an external source.

     Accordingly, any potential control character, apart from \n, is removed from a declaration's
     value during the conversion process to XML before the value itself is handled by the default
     JAXB's handler, MinimumEscapeHandler.
   */
  override def escape(array: Array[Char], from: Int, length: Int, isAttVal: Boolean, writer: Writer): Unit = {
    var ix = from
    var len = length
    while (ix < len) {
      if (Character.isISOControl(array(ix)) && array(ix) != '\n') {
        len -= 1
        for (ix2 <- ix until len) array(ix2) = array(ix2 + 1)
        array(len) = ' '
        ix -= 1
      }
      ix += 1
    }

    MinimumEscapeHandler.theInstance.escape(array, from, len, isAttVal, writer)
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
    value.foreach(chr => if (!Character.isISOControl(chr)) sb += chr)
    sb.toString
  }
}
