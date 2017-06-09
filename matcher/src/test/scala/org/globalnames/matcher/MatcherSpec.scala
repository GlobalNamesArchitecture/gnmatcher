package org.globalnames
package matcher

class MatcherSpec extends SpecConfig {
  "find matches correctly" in {
    val matcher = Matcher(Seq("Aaadonta angaurana",
                              "Aaadonta constricta",
                              "Aaadonta constricta babelthuapi",
                              "Abacetus cyclomous",
                              "Abacetus cyclomus"))
    matcher.findMatches("Aaadonta angaurana") should contain only Candidate("Aaadonta angaurana", 0)
    matcher.findMatches("Aaadonta angauranA") should contain only Candidate("Aaadonta angaurana", 0)
    matcher.findMatches("AaadontX angauranX") should contain only Candidate("Aaadonta angaurana", 2)
    matcher.findMatches("Abacetus cyclomoXX") should contain only (
      Candidate("Abacetus cyclomus", 3), Candidate("Abacetus cyclomous", 2)
    )
  }
}
