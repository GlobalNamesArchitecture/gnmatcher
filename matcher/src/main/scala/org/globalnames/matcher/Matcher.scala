package org.globalnames.matcher


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
  def apply(nameToDatasourceIdsMap: Map[String, Set[Int]]): Matcher = {
    val stemMatcher = StemMatcher(nameToDatasourceIdsMap)
    val verbatimMatcher = VerbatimMatcher(nameToDatasourceIdsMap)

    new Matcher(nameToDatasourceIdsMap, verbatimMatcher, stemMatcher)
  }
}
