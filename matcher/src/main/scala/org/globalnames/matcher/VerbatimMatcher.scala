package org.globalnames
package matcher

import com.BoxOfC.LevenshteinAutomaton.LevenshteinAutomaton
import com.BoxOfC.MDAG.MDAG
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scalaz.syntax.std.option._

private[matcher]
class VerbatimMatcher(wordToDatasources: Map[String, Set[Int]],
                      wordVerbatimToWords: mutable.Map[String, Set[String]],
                      mdag: MDAG) {
  private val maxEditDistance = 2

  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    val wordVerbatim = VerbatimMatcher.transform(word)
    val wordStem = StemMatcher.transform(word)
    val verbatimMatches = LevenshteinAutomaton.tableFuzzySearch(maxEditDistance, wordVerbatim, mdag)

    val result = for {
      verbatimMatch <- verbatimMatches.toVector
      fullWord <- wordVerbatimToWords(verbatimMatch)
      fullWordDataSource <- wordToDatasources(fullWord)
      if dataSources.isEmpty || dataSources.contains(fullWordDataSource)
    } yield {
      val wordStemMatch = StemMatcher.transform(verbatimMatch)
      Candidate(stem = wordStemMatch, term = fullWord, dataSourceId = fullWordDataSource,
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

    for (((word, _), idx) <- wordToDatasources.zipWithIndex) {
      if (idx > 0 && idx % 10000 == 0) {
        logger.info(s"Verbatim matcher (progress): $idx")
      }

      val wordVerbatim = transform(word)
      wordVerbatims += wordVerbatim
      wordVerbatimToWords +=
        wordVerbatim -> (wordVerbatimToWords.getOrElse(wordVerbatim, Set()) + word)
    }

    val mdag = new MDAG(wordVerbatims.sorted)
    val vm = new VerbatimMatcher(wordToDatasources, wordVerbatimToWords, mdag)
    vm
  }

  def transform(word: String): String = {
    val wordTransformed = word.toLowerCase
                              .replace('j', 'i')
                              .replace('v', 'u')
    wordTransformed
  }
}
