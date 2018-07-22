package org.globalnames
package matcher

import akka.http.impl.util.EnhancedString
import com.BoxOfC.LevenshteinAutomaton.LevenshteinAutomaton
import com.typesafe.scalalogging.Logger
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._

import scala.collection.mutable

private[matcher]
class GenusMatcher private (wordToDatasources: Map[String, Set[Int]],
                            genusToFullWords: Map[String, Set[String]]) {

  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    if (!GenusMatcher.valid(word)) {
      Vector.empty
    } else {
      val wordGenus = GenusMatcher.transform(word)
      val wordStem = StemMatcher.transform(word)

      val result = for {
        fullWord <- genusToFullWords.getOrElse(wordGenus, Set())
        fullWordDataSourcesFound = {
          val ds = wordToDatasources(fullWord)
          dataSources.isEmpty ? ds | ds.intersect(dataSources)
        }
        if fullWordDataSourcesFound.nonEmpty
      } yield {
        val wordStemMatch = StemMatcher.transform(fullWord)
        Candidate(stem = wordStemMatch, term = fullWord, dataSourceIds = fullWordDataSourcesFound,
          verbatimEditDistance =
            LevenshteinAutomaton.computeEditDistance(word, fullWord).some,
          stemEditDistance =
            LevenshteinAutomaton.computeEditDistance(wordStemMatch, wordStem).some)
      }
      result.toVector
    }
  }

}

object GenusMatcher {
  private[GenusMatcher] val logger = Logger[GenusMatcher]

  def apply(wordToDatasources: Map[String, Set[Int]]): GenusMatcher = {
    val genusToFullWords = mutable.Map.empty[String, Set[String]]

    for { word <- wordToDatasources.keys if valid(word) } {
      val wordGenus = transform(word)
      val fullWords = genusToFullWords.getOrElse(wordGenus, Set())
      genusToFullWords += wordGenus -> (fullWords + word)
    }

    new GenusMatcher(wordToDatasources, Map(genusToFullWords.toList: _*))
  }

  def valid(word: String): Boolean = {
    word.indexOf(' ') == -1 && word.last != '.'
  }

  def transform(word: String): String = {
    val wordTransformed = word.toLowerCase
                              .replace('j', 'i')
                              .replace('v', 'u')
    wordTransformed
  }
}
