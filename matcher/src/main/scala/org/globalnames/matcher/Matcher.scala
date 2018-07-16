package org.globalnames.matcher


class Matcher private(nameToDatasourceIdsMap: Map[String, Set[Int]],
                      stemMatcher: StemMatcher) {

  def findMatches(verbatim: String, dataSources: Set[Int]): Vector[Candidate] = {
    stemMatcher.findMatches(verbatim, dataSources)
  }
}

object Matcher {
  def apply(nameToDatasourceIdsMap: Map[String, Set[Int]]): Matcher = {
    val stemMatcher = StemMatcher(nameToDatasourceIdsMap)

    new Matcher(nameToDatasourceIdsMap, stemMatcher)
  }
}
