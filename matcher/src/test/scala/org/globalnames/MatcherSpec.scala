package org.globalnames

import java.io.File

class MatcherSpec extends SpecConfig {
  "must transduce correctly" in {
    val matcher = Matcher(Seq("Abdf", "Abce", "Dddd"), 2)
    val candidates = matcher.transduce("Abc")
    candidates should contain only (Candidate("Abce", 1), Candidate("Abdf", 1))
  }

  val dumpPath = "MatcherSpec.ser"

  "must dump and restore itself" in {
    val matcher = Matcher(Seq("Abdf", "Abce", "Dddd"), 2)
    matcher.dump(dumpPath)

    new File(dumpPath) should exist

    val matcherRestored = Matcher.restore(dumpPath)
    val candidates = matcherRestored.transduce("Abc")
    candidates should contain only (Candidate("Abce", 1), Candidate("Abdf", 1))
  }

  override def afterAll(): Unit = {
    val dumpFile = new File(dumpPath)
    if (dumpFile.exists) {
      dumpFile.delete()
    }
  }
}
