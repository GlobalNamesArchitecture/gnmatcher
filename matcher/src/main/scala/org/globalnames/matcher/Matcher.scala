package org.globalnames
package matcher

import com.BoxOfC.LevenshteinAutomaton.LevenshteinAutomaton
import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.StringUtils
import akka.http.impl.util.EnhancedString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.syntax.std.boolean._

class SimpleMatcher private(verbatimMatcher: VerbatimMatcher,
                            stemMatcher: StemMatcher) {

  def completed: Future[Unit] =
    for (_ <- verbatimMatcher.completed; _ <- stemMatcher.completed) yield ()

  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    val matchesByStem = stemMatcher.findMatches(word, dataSources)
    if (matchesByStem.nonEmpty) {
      matchesByStem
    } else {
      val matchesByVerbatim = verbatimMatcher.findMatches(word, dataSources)
      matchesByVerbatim
    }
  }
}

object SimpleMatcher {
  def apply(verbatimMatcher: VerbatimMatcher, stemMatcher: StemMatcher): SimpleMatcher = {
    new SimpleMatcher(verbatimMatcher, stemMatcher)
  }
}

class Matcher(simpleMatcher: SimpleMatcher,
              abbreviationMatcher: AbbreviationMatcher,
              genusMatcher: GenusMatcher) {

  def completed: Future[Unit] = simpleMatcher.completed

  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    val finder: (String, Set[Int]) => Vector[Candidate] =
      if (GenusMatcher.valid(word)) {
        genusMatcher.findMatches
      } else if (AbbreviationMatcher.transform(word).valid) {
        abbreviationMatcher.findMatches
      } else {
        simpleMatcher.findMatches
      }

    finder(word, dataSources)
  }
}

object Matcher {
  private[Matcher] val logger = Logger[Matcher]

  private[Matcher]
  def matchingThresholdHelper(wordInput: String, wordCandidate: String,
                              spaceEdits: Array[Int]): Boolean = {
    val wordInputParts = wordInput.fastSplit(' ')
    val wordCandidateParts = wordCandidate.fastSplit(' ')

    if (wordInputParts.length != wordCandidateParts.length) {
      logger.error(s"$wordInput and $wordCandidate have different count of parts")
    }

    val result = wordInputParts.indices.foldLeft(true) { case (ok, idx) =>
      if (!ok) {
        false
      } else {
        val wordInputPart = wordInputParts(idx)
        val wordCandidatePart = wordCandidateParts(idx)

        val allowedEdits = wordInputPart.length match {
          case x if x < 6 => 0
          case x if x < 11 => 1
          case _ => 2
        }

        val actualEdits =
          LevenshteinAutomaton.computeEditDistance(wordInputPart, wordCandidatePart)

        actualEdits + spaceEdits(idx) <= allowedEdits
      }
    }
    result
  }

  def matchingThreshold(wordInput: String, wordCandidate: String): Boolean = {
    val wordInputSpaces = StringUtils.countMatches(wordInput, ' ')
    val wordCandidateSpaces = StringUtils.countMatches(wordCandidate, ' ')

    if (wordInputSpaces == wordCandidateSpaces) {
      matchingThresholdHelper(wordInput, wordCandidate, Array.fill(wordInputSpaces + 1)(0))
    } else {
      val (wordLong, wordShort, wordLongSpaces, wordShortSpaces) =
        if (wordInputSpaces > wordCandidateSpaces) {
          (wordInput, wordCandidate, wordInputSpaces, wordCandidateSpaces)
        } else {
          (wordCandidate, wordInput, wordCandidateSpaces, wordInputSpaces)
        }

      val wordLongParts = wordLong.fastSplit(' ')
      val result = Vector.range(0, wordLongSpaces)
                         .combinations(wordShortSpaces)
                         .foldLeft(false) {
        (ok, comb) =>
          if (ok) {
            true
          } else {
            var wordLongIdx = 0
            val wordLongNew = new StringBuilder()
            val spaceEdits = Array.fill(wordShortSpaces + 1)(-1)
            for ((c, idx) <- comb.zipWithIndex) {
              while (wordLongIdx <= c) {
                wordLongNew.append(wordLongParts(wordLongIdx))
                spaceEdits(idx) += 1
                wordLongIdx += 1
              }
              wordLongNew.append(' ')
            }
            val slice = wordLongParts.slice(wordLongIdx, wordLongParts.length)
            slice.foreach { wordLongNew.append }
            spaceEdits(spaceEdits.length - 1) = slice.length - 1

            val (wordInputNew, wordCandidateNew) =
              (wordInputSpaces > wordCandidateSpaces) ?
                ((wordLongNew.toString, wordShort)) |
                ((wordShort, wordLongNew.toString))

            matchingThresholdHelper(wordInputNew, wordCandidateNew, spaceEdits)
          }
      }
      result
    }
  }

  def apply(nameToDatasourceIdsMap: Map[String, Set[Int]]): Matcher = {
    logger.info("Creating StemMatcher")
    val stemMatcher = StemMatcher(nameToDatasourceIdsMap)
    logger.info("StemMatcher created")

    logger.info("Creating VerbatimMatcher")
    val verbatimMatcher = VerbatimMatcher(nameToDatasourceIdsMap)
    logger.info("VerbatimMatcher created")

    logger.info("Creating AbbreviationMatcher")
    val abbreviationMatcher = AbbreviationMatcher(nameToDatasourceIdsMap)
    logger.info("AbbreviationMatcher created")

    logger.info("Creating AbbreviationMatcher")
    val genusMatcher = GenusMatcher(nameToDatasourceIdsMap)
    logger.info("AbbreviationMatcher created")

    val simpleMatcher = SimpleMatcher(verbatimMatcher, stemMatcher)
    val matcher = new Matcher(simpleMatcher, abbreviationMatcher, genusMatcher)
    matcher
  }
}
