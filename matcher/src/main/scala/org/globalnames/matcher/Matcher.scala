package org.globalnames.matcher

import com.typesafe.scalalogging.Logger


class SimpleMatcher private(verbatimMatcher: VerbatimMatcher,
                            stemMatcher: StemMatcher) {

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
              abbreviationMatcher: AbbreviationMatcher) {
  def findMatches(word: String, dataSources: Set[Int]): Vector[Candidate] = {
    val finder: (String, Set[Int]) => Vector[Candidate] =
      if (AbbreviationMatcher.transform(word).valid) {
        abbreviationMatcher.findMatches
      } else {
        simpleMatcher.findMatches
      }

    finder(word, dataSources)
  }
}

object Matcher {
  private[Matcher] val logger = Logger[Matcher]

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

    val simpleMatcher = SimpleMatcher(verbatimMatcher, stemMatcher)
    val matcher = new Matcher(simpleMatcher, abbreviationMatcher)
    matcher
  }
}
