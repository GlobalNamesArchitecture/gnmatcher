package org.globalnames
package matcher

import scala.io.Source

class LatinStemmerSpec extends SpecConfig {
  case class TestDataEntry(word: String, nounForm: String)

  val testDataEntries: Vector[TestDataEntry] =
    Source.fromURL(getClass.getResource("/latin_words.txt"), "UTF-8")
      .getLines
      .map { line =>
        val entities = line.split("\\s+")
        TestDataEntry(entities(0), entities(1))
      }
      .toVector

  "extract noun-form stems correctly from" in {
    val stems = testDataEntries.map { tde => LatinStemmer.stem(tde.word) }
    val expected = testDataEntries.map { _.nounForm }
    expected.diff(stems) shouldBe empty
  }
}
