package org.globalnames
package matcher

import scala.io.Source

class LatinStemmerSpec extends SpecConfig {
  import LatinStemmer.stemmize

  case class TestDataEntry(word: String, nounForm: String)

  val testDataEntries: Vector[TestDataEntry] =
    Source.fromURL(getClass.getResource("/latin_words.txt"), "UTF-8")
      .getLines
      .map { line =>
        val entities = line.split("\\s+")
        TestDataEntry(entities(0), entities(1))
      }
      .toVector

  "correctly extract noun-form stems" in {
    testDataEntries.foreach { tde =>
      val word = stemmize(tde.word)
      withClue(s"${tde.word}: ") { word.mappedStem shouldBe tde.nounForm }
    }
  }

  "correctly extract original and mapped stems of exact size" in {
    testDataEntries.foreach { tde =>
      val word = stemmize(tde.word)
      withClue(s"${word.originalStem} & ${word.mappedStem}: ") {
        word.originalStem.length shouldBe word.mappedStem.length
      }
    }
  }

  "correctly extract noun-form suffixes" in {
    testDataEntries.foreach { tde =>
      val word = stemmize(tde.word)
      withClue(s"$word: ") {
        (word.originalStem + word.suffix) shouldBe tde.word
      }
    }
  }
}
