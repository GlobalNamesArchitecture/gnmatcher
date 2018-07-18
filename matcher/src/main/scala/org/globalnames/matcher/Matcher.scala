package org.globalnames.matcher

import com.typesafe.scalalogging.Logger


class Matcher private(nameToDatasourceIdsMap: Map[String, Set[Int]],
                      verbatimMatcher: VerbatimMatcher,
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

object Matcher {
  private[Matcher] val logger = Logger[Matcher]

  def apply(nameToDatasourceIdsMap: Map[String, Set[Int]]): Matcher = {
    logger.info("Creating StemMatcher")
    val stemMatcher = StemMatcher(nameToDatasourceIdsMap)
    logger.info("StemMatcher created")

    logger.info("Creating VerbatimMatcher")
    val verbatimMatcher = VerbatimMatcher(nameToDatasourceIdsMap)
    logger.info("VerbatimMatcher created")

    new Matcher(nameToDatasourceIdsMap, verbatimMatcher, stemMatcher)
  }
}
