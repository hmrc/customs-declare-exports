/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.exports.syntax

import java.util.NoSuchElementException

import scala.reflect.ClassTag

import org.scalactic.source.Position

class ScenarioContext(values: Map[Class[_], (Any, Position)]) {
  def updated[T](value: T)(implicit tag: ClassTag[T], position: Position): ScenarioContext =
    new ScenarioContext(values.updated(tag.runtimeClass, (value, position)))
  def get[T](implicit tag: ClassTag[T]): T = {
    val clazz = tag.runtimeClass
    maybe[T].getOrElse {
      val entries = values.map {
        case (key, (_, position)) => s"${key.getSimpleName} set at ${position.fileName}:${position.lineNumber}"
      }.mkString("\n\t", "\n\t", "\n")

      throw new NoSuchElementException(s"There is no entry for class ${clazz.getSimpleName}. Now we have $entries")
    }
  }

  def maybe[T](implicit tag: ClassTag[T]): Option[T] = {
    val clazz = tag.runtimeClass
    values.get(clazz).map(_._1.asInstanceOf[T])
  }
}
