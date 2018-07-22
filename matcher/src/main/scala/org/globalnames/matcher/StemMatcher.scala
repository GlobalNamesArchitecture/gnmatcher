package org.globalnames
package matcher

import akka.http.impl.util.EnhancedString
import com.BoxOfC.LevenshteinAutomaton.LevenshteinAutomaton
import com.BoxOfC.MDAG.MDAG
import com.typesafe.scalalogging.Logger
import org.globalnames.matcher.util._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private[matcher]
class StemMatcher private(wordToDatasources: Map[String, Set[Int]],
                          wordStemToWords: mutable.Map[String, Set[String]],
                          mdagFut: Future[MDAG]) {

  private val maxEditDistance = 2

  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    val wordStem = StemMatcher.transform(word)
    val stemMatches =
      LevenshteinAutomaton.tableFuzzySearch(maxEditDistance, wordStem, mdagFut.valueOrEmpty)

    val result = for {
      stemMatch <- stemMatches.toVector
      fullWord <- wordStemToWords(stemMatch)
      fullWordDataSourcesFound = {
        val ds = wordToDatasources(fullWord)
        dataSources.isEmpty ? ds | ds.intersect(dataSources)
      }
      if fullWordDataSourcesFound.nonEmpty
    } yield Candidate(stem = stemMatch, term = fullWord, dataSourceIds = fullWordDataSourcesFound,
                      verbatimEditDistance =
                        LevenshteinAutomaton.computeEditDistance(word, fullWord).some,
                      stemEditDistance =
                        LevenshteinAutomaton.computeEditDistance(wordStem, stemMatch).some)
    result
  }
}

object StemMatcher {
  private[StemMatcher] val logger = Logger[StemMatcher]

  def apply(wordToDatasources: Map[String, Set[Int]]): StemMatcher = {
    val wordStemToWords = mutable.Map.empty[String, Set[String]]
    val wordStems = ArrayBuffer[String]()

    for (((word, _), idx) <- wordToDatasources.zipWithIndex) {
      if (idx > 0 && idx % 10000 == 0) {
        logger.info(s"Stem matcher (progress): $idx")
      }

      val wordStem = transform(word)
      wordStems += wordStem
      wordStemToWords += wordStem -> (wordStemToWords.getOrElse(wordStem, Set()) + word)
    }

    val mdagFut = Future { new MDAG(wordStems.sorted) }
    val sm = new StemMatcher(wordToDatasources, wordStemToWords, mdagFut)
    sm
  }

  def transform(word: String): String = {
    val wordParts = word.toLowerCase.fastSplit(delimiter = ' ')
    (wordParts.length < 2) ?
      word | wordParts.map { w => LatinStemmer.stemmize(w).mappedStem }.mkString(" ")
  }
}
