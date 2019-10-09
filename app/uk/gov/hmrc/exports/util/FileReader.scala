package uk.gov.hmrc.exports.util

import javax.inject.{Inject, Singleton}

import scala.io.Source

@Singleton
class FileReader @Inject()(){

  def readLines(filename: String): List[String] = {
    val source = Source.fromFile(filename)
    try {
      source
        .getLines().toList
    } finally source.close()
  }

}
