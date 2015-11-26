package org.globalnames

import org.specs2.mutable.Specification

class MatcherSpec extends Specification {
  "Matcher".p

  "must transduce correctly" in {
    val matcher = new Matcher(Seq("Abdf", "Abce", "Dddd"), 2)
    val candidates = matcher.transduce("Abc")
    candidates must containTheSameElementsAs(Seq(
      Candidate("Abce", 1), Candidate("Abdf", 1)))
  }
}
