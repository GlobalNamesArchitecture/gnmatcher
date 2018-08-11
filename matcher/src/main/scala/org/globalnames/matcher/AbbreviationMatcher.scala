package org.globalnames
package matcher

import akka.http.impl.util.EnhancedString
import com.BoxOfC.LevenshteinAutomaton.LevenshteinAutomaton
import com.typesafe.scalalogging.Logger
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._

import scala.collection.mutable

private[matcher]
case class AbbreviationMatcherByLetter(simpleMatcher: SimpleMatcher,
                                       verbatimRestToWords: Map[String, Set[String]],
                                       verbatimRestToDatasources: Map[String, Set[Int]])

private[matcher]
class AbbreviationMatcher(letterToMatchers: Map[String, AbbreviationMatcherByLetter]) {
  def findMatches(wordTransformed: AbbreviatedWord, dataSources: Set[Int]): Vector[Candidate] = {
    Vector.empty
  }
}

case class AbbreviatedWord(letterOpt: Option[String],
                           originalWord: String,
                           restStemmed: String, restVerbatim: String) {
  def valid: Boolean = letterOpt.isDefined
  val letter: String = letterOpt.getOrElse("")
  def stemmed: String = s"$letter. $restStemmed"
}

object AbbreviationMatcher {
  private[AbbreviationMatcher] val logger = Logger[AbbreviationMatcher]

  def apply(wordToDatasources: Map[String, Set[Int]]): AbbreviationMatcher = {
    new AbbreviationMatcher(Map.empty)
  }

  def transformInputWord(word: String): AbbreviatedWord = {
    val wordParts = word.toLowerCase
                        .replace('j', 'i')
                        .replace('v', 'u')
                        .fastSplit(' ')
    if (wordParts.head.length > 2 ||
        wordParts.head.last != '.') {
      AbbreviatedWord(None, "", "", "")
    } else {
      val restStemmed =
        wordParts.tail.map { w => LatinStemmer.stemmize(w).mappedStem }.mkString(" ")
      val restVerbatim =
        wordParts.tail.mkString(" ")
      val letterOnly = wordParts.head.dropRight(1)
      AbbreviatedWord(letterOnly.some, word, restStemmed, restVerbatim)
    }
  }

  def transformInitWord(word: String): AbbreviatedWord = {
    val wordParts = word.toLowerCase
                        .replace('j', 'i')
                        .replace('v', 'u')
                        .fastSplit(' ')
    if (wordParts.isEmpty ||
        wordParts.length > 4 ||
        wordParts.exists { x => x.last == '.' }) {
      AbbreviatedWord(None, "", "", "")
    } else {
      val restStemmed =
        wordParts.tail.map { w => LatinStemmer.stemmize(w).mappedStem }.mkString(" ")
      val restVerbatim =
        wordParts.tail.mkString(" ")
      val letterOnly = wordParts.head.substring(0, 1)
      AbbreviatedWord(letterOnly.some, word, restStemmed, restVerbatim)
    }
  }
}
