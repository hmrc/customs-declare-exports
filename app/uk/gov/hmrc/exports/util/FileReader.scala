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

package uk.gov.hmrc.exports.util

import javax.inject.{Inject, Singleton}

import scala.io.Source

@Singleton
class FileReader @Inject() () {

  def readLines(filename: String, skipHeaderLine: Boolean = false): List[String] = {
    val lines = read(filename)
      .filter(_.nonEmpty) // Ignore empty lines
      .filter(!_.startsWith("#")) // Ignore comment lines
    if (skipHeaderLine) lines.tail else lines
  }

  private def read(filename: String): List[String] = {
    val source = Source.fromURL(getClass.getClassLoader.getResource(filename), "UTF-8")
    try
      source.getLines().toList
    finally source.close()
  }

}
