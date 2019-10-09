package uk.gov.hmrc.exports.util

import javax.inject.{Inject, Singleton}

import scala.io.Source

@Singleton
class FileReader @Inject()(){

  def readLines(filename: String, skipHeaderLine: Boolean = false): List[String] = {
    val lines = read(filename)
      .filter(_.nonEmpty) // Ignore empty lines
      .filter(!_.startsWith("#")) // Ignore comment lines
    if(skipHeaderLine) lines.tail else lines
  }

  private def read(filename: String): List[String] = {
    val source = Source.fromFile(filename)
    try {
      source.getLines().toList
    } finally source.close()
  }

}
