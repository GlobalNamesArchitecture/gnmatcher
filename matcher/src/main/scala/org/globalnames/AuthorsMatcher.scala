package org.globalnames

case class Author(name: String)

/**
  Port of
  https://github.com/GlobalNamesArchitecture/taxamatch_rb/blob/master/lib/taxamatch_rb/authmatch.rb
*/
object AuthorsMatcher {
  private val dlm = new DamerauLevenshteinAlgorithm(1, 1, 1, 1)

  def score(authors1: Seq[Author], year1: Option[Int],
            authors2: Seq[Author], year2: Option[Int]): Double = {
    val (authsShort, authsLong) =
      if (authors1.size > authors2.size) (authors2, authors1)
      else (authors1, authors2)
    val authsLikelihood =
      authsShort.map { aSh => authsLong.map { aLo => authorsScore(aSh, aLo) }.max }
                .filter { _ > 0.01 }
    if (authsLikelihood.isEmpty) 0.0
    else authsLikelihood.product * yearsScore(year1, year2)
  }

  def authorsScore(a1: Author, a2: Author): Double = {
    if (a1.name == a2.name) 1.0
    else {
      def dropPoint(name: String) =
        if (name.endsWith(".")) name.substring(0, name.length - 2)
        else name

      val a1pointFree = dropPoint(a1.name)
      val a2pointFree = dropPoint(a2.name)

      if (a1pointFree.startsWith(a2pointFree) || a2pointFree.startsWith(a1pointFree)) 1.0
      else {
        val distance = dlm.execute(a1pointFree, a2pointFree)
        if (distance <= 3) (4 - distance) / 4.0
        else 0.0
      }
    }
  }

  private[globalnames] def yearsScore(year1: Option[Int], year2: Option[Int]): Double = {
    (year1, year2) match {
      case (None, None) => 1.0
      case (Some(_), None) | (None, Some(_)) => 0.5
      case (Some(y1), Some(y2)) =>
        val yearGap = 100.0
        val y = math.abs(y1 - y2)
        if (y >= yearGap) 0.0
        else if (y <= 2) 1.0
        else 1.0 - y / yearGap
    }
  }
}
