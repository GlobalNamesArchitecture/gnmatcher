package org.globalnames

import java.io.File

import org.specs2.mutable.Specification
import org.specs2.matcher.FileMatchers

class MatcherSpec extends Specification with FileMatchers  {
  "Matcher".p

  "must transduce correctly" in {
    val matcher = Matcher(Seq("Abdf", "Abce", "Dddd"), 2)
    val candidates = matcher.transduce("Abc")
    candidates must containTheSameElementsAs(Seq(
      Candidate("Abce", 1), Candidate("Abdf", 1)))
  }

  val dumpPath = "MatcherSpec.ser"

  "must dump and restore itself" in {
    val matcher = Matcher(Seq("Abdf", "Abce", "Dddd"), 2)
    matcher.dump(dumpPath)

    dumpPath must beAnExistingPath

    val matcherRestored = Matcher.restore(dumpPath)
    val candidates = matcherRestored.transduce("Abc")
    candidates must containTheSameElementsAs(Seq(
      Candidate("Abce", 1), Candidate("Abdf", 1)))
  }

  step {
    val dumpFile = new File(dumpPath)
    if (dumpFile.exists) {
      dumpFile.delete()
    }
  }
}
