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
  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    val wordTransformed = AbbreviationMatcher.transform(word)
    if (!wordTransformed.valid || !letterToMatchers.contains(wordTransformed.letter)) {
      Vector.empty
    } else {
      val matcher = letterToMatchers(wordTransformed.letter)
      val matches = matcher.simpleMatcher.findMatches(wordTransformed.restVerbatim, dataSources)
      val wordStem = StemMatcher.transform(word)

      val result = for {
        mtch <- matches.map { _.term }.distinct
        fullWord <- matcher.verbatimRestToWords(mtch)
        fullWordDataSourcesFound = {
          val ds = matcher.verbatimRestToDatasources(mtch)
          dataSources.isEmpty ? ds | ds.intersect(dataSources)
        }
        if fullWordDataSourcesFound.nonEmpty
      } yield {
        val wordStemMatch = StemMatcher.transform(fullWord)
        Candidate(stem = wordStemMatch, term = fullWord, dataSourceIds = fullWordDataSourcesFound,
          verbatimEditDistance =
            LevenshteinAutomaton.computeEditDistance(word, fullWord).some,
          stemEditDistance =
            LevenshteinAutomaton.computeEditDistance(wordStem, wordStemMatch).some)
      }
      result
    }
  }
}

case class AbbreviatedWord(letterOpt: Option[String],
                           originalWord: String,
                           restStemmed: String, restVerbatim: String) {
  def valid: Boolean = letterOpt.isDefined
  val letter: String = letterOpt.getOrElse("")
}

object AbbreviationMatcher {
  private[AbbreviationMatcher] val logger = Logger[AbbreviationMatcher]

  def apply(wordToDatasources: Map[String, Set[Int]]): AbbreviationMatcher = {
    val letterToVerbatimsRest = mutable.Map.empty[String, mutable.Map[String, Set[String]]]

    for (((word, _), idx) <- wordToDatasources.zipWithIndex) {
      if (idx > 0 && idx % 10000 == 0) {
        logger.info(s"Abbreviation matcher (progress): $idx")
      }

      val abbreviatedWord = transform(word)

      if (abbreviatedWord.valid) {
        val ltv = letterToVerbatimsRest.getOrElse(abbreviatedWord.letter, mutable.Map.empty)
        ltv += abbreviatedWord.restVerbatim ->
          (ltv.getOrElse(abbreviatedWord.restVerbatim, Set()) + word)
        letterToVerbatimsRest += abbreviatedWord.letter -> ltv
      }
    }

    val result = for (letter <- letterToVerbatimsRest.keys) yield {
      val verbatimRestToFullwords = Map(letterToVerbatimsRest(letter).toList: _*)
      val verbatimRestToDatasources =
        for {
          (verbatimRest, names) <- verbatimRestToFullwords
          name <- names
        } yield verbatimRest -> wordToDatasources(name)
      val vm = VerbatimMatcher(verbatimRestToDatasources)
      val sm = StemMatcher(verbatimRestToDatasources)

      val ambl = AbbreviationMatcherByLetter(
        SimpleMatcher(vm, sm),
        verbatimRestToFullwords,
        verbatimRestToDatasources
      )

      letter -> ambl
    }
    new AbbreviationMatcher(result.toMap)
  }

  def transform(word: String): AbbreviatedWord = {
    val wordParts = word.toLowerCase.fastSplit(' ')
    if (wordParts.isEmpty ||
        wordParts.head.last != '.' ||
        wordParts.head.length > 2 ||
        wordParts.length < 2) {
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
}