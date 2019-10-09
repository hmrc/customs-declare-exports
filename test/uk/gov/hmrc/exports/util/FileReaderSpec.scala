package uk.gov.hmrc.exports.util

import org.scalatest.{MustMatchers, WordSpec}

class FileReaderSpec extends WordSpec with MustMatchers {

  private val reader = new FileReader()

  "Read lines" should {
    "not skip header by default" in {
      reader.readLines("header-file.csv").head mustBe "header-line"
    }

    "skip header" in {
      reader.readLines("header-file.csv", skipHeaderLine = true).head mustBe "data"
    }
  }

}
