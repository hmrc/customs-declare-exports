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

package uk.gov.hmrc.exports.services.decversions

object ExtractAmendmentChanges {

  import scala.xml._

  // Load the XML document
  val xmlString = """
<root>
  <level1>
    <level2>
      <element>Item 1</element>
      <element>Item 2</element>
      <element>Item 3</element>
    </level2>
    <level2>
      <element>Item 4</element>
      <element>Item 5</element>
      <element>Item 6</element>
    </level2>
  </level1>
</root>
"""
  val xml = XML.loadString(xmlString)

  // Define the XPath-like expression
  val expression = "level1/level2/element"

  // Get the second element in the second sequence
  val result = (xml \\ expression)(1)(1).text

  println(result) // Outputs: "Item 5"

}
