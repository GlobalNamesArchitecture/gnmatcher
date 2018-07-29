package org.globalnames
package matcher

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
import akka.http.impl.util.EnhancedString

private[matcher]
class VerbatimMatcher(wordToDatasources: Map[String, Set[Int]],
                      wordVerbatimToWords: mutable.Map[String, Set[String]],
                      mdagFut: Future[MDAG]) {
  private val maxEditDistance = 2

  def completed: Future[Unit] = mdagFut.map { _ => () }

  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    val wordVerbatim = VerbatimMatcher.transform(word)
    val wordStem = StemMatcher.transform(word)
    val verbatimMatches =
      LevenshteinAutomaton.tableFuzzySearch(maxEditDistance, wordVerbatim, mdagFut.valueOrEmpty)

    val result = for {
      verbatimMatch <- verbatimMatches.toVector
      if Matcher.matchingThreshold(wordVerbatim, verbatimMatch)
      fullWord <- wordVerbatimToWords(verbatimMatch)
      fullWordDataSourcesFound = {
        val ds = wordToDatasources(fullWord)
        dataSources.isEmpty ? ds | ds.intersect(dataSources)
      }
      if fullWordDataSourcesFound.nonEmpty
    } yield {
      val wordStemMatch = StemMatcher.transform(verbatimMatch)
      Candidate(stem = wordStemMatch, term = fullWord, dataSourceIds = fullWordDataSourcesFound,
        verbatimEditDistance =
          LevenshteinAutomaton.computeEditDistance(word, fullWord).some,
        stemEditDistance =
          LevenshteinAutomaton.computeEditDistance(wordStemMatch, wordStem).some)
    }
    result
  }
}

object VerbatimMatcher {
  private[VerbatimMatcher] val logger = Logger[VerbatimMatcher]

  def apply(wordToDatasources: Map[String, Set[Int]]): VerbatimMatcher = {
    val wordVerbatimToWords = mutable.Map.empty[String, Set[String]]
    val wordVerbatims = ArrayBuffer[String]()

    for ((word, idx) <- wordToDatasources.keys.toVector.zipWithIndex) {
      if (idx > 0 && idx % 100000 == 0) {
        logger.info(s"Verbatim matcher (progress): $idx")
      }

      val wordVerbatim = transform(word)
      wordVerbatims += wordVerbatim
      wordVerbatimToWords +=
        wordVerbatim -> (wordVerbatimToWords.getOrElse(wordVerbatim, Set()) + word)
    }

    val mdagFut = Future { new MDAG(wordVerbatims.sorted) }
    mdagFut.onComplete { _ => logger.info("MDAG constructed") }
    val vm = new VerbatimMatcher(wordToDatasources, wordVerbatimToWords, mdagFut)
    vm
  }

  def transform(word: String): String = {
    val wordTransformed = word.toLowerCase
                              .replace('j', 'i')
                              .replace('v', 'u')
    wordTransformed
  }
}
