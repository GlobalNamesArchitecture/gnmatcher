package org.globalnames
package matcher

class MatcherSpec extends SpecConfig {
  "must transduce correctly" in {
    val matcher = Matcher(Seq("Abdf", "Abce", "Dddd"), 2)
    val candidates = matcher.transduce("Abc")
    candidates should contain only (Candidate("Abce", 1), Candidate("Abdf", 1))
  }
}
