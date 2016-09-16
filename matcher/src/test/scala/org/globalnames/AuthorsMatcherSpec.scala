package org.globalnames

class AuthorsMatcherSpec extends SpecConfig {
  "calculate score" when {
    "found authors by abbreviation" in {
      AuthorsMatcher.score(Seq(Author("Linnaeus"), Author("Muller")), None,
                           Seq(Author("L")), Some(1788)) shouldBe 0.5 +- 0.001
    }

    "no authors" in {
      AuthorsMatcher.score(Seq(Author("Linnaeus"), Author("Kurtz")), None,
                           Seq(), None) shouldBe 0.0 +- 0.01
    }

    "all authors, same year" in {
      AuthorsMatcher.score(
        Seq(Author("Linnaeus"), Author("Muller")), Some(1767),
        Seq(Author("Muller"), Author("Linnaeus")), Some(1767)) shouldBe 1.0 +- 0.001
    }

    "all authors, year diff" in {
      AuthorsMatcher.score(
        Seq(Author("Linnaeus"), Author("Muller")), Some(1767),
        Seq(Author("Muller"), Author("Linnaeus")), Some(1764)) shouldBe 0.97 +- 0.001
    }

    "year is not counted in" in {
      AuthorsMatcher.score(Seq(Author("Linnaeus"), Author("Muller")), Some(1767),
                           Seq(Author("Muller"), Author("Linnaeus")), None) shouldBe 0.5 +- 0.001
    }

    "found all authors on one side, same year" in {
      AuthorsMatcher.score(
        Seq(Author("Linnaeus"), Author("Muller"), Author("Kurtz")), Some(1767),
        Seq(Author("Muller"), Author("Linnaeus")), Some(1767)) shouldBe 1.0 +- 0.001
    }

    "found all authors on one side, year diff" in {
      AuthorsMatcher.score(
        Seq(Author("Linnaeus"), Author("Muller"), Author("Kurtz")), Some(1767),
        Seq(Author("Muller"), Author("Linnaeus")), Some(1760)) shouldBe 0.93 +- 0.001
    }

    "found all authors on one side, year does not count" in {
      AuthorsMatcher.score(
        Seq(Author("Linnaeus"), Author("Muller"), Author("Kurtz")), Some(1767),
        Seq(Author("Muller"), Author("Linnaeus")), None) shouldBe 0.5 +- 0.001
    }

    "found some authors" in {
      AuthorsMatcher.score(
        Seq(Author("Linnaeus"), Author("Muller"), Author("Kurtz")), Some(1766),
        Seq(Author("Muller"), Author("Kurtz"), Author("Stepanov")), None) shouldBe 0.5 +- 0.001
    }

    "if year does not match or not present no match for previous case" in {
      AuthorsMatcher.score(
      Seq(Author("Stepanov"), Author("Linnaeus"), Author("Muller")), Some(1766),
      Seq(Author("Muller"), Author("Kurtz"), Author("Stepanov")), Some(1760)) shouldBe 0.94 +- 0.001
    }
  }

  "compare years" in {
    AuthorsMatcher.yearsScore(Some(1882), Some(1880)) shouldBe 1.0 +- .01
    AuthorsMatcher.yearsScore(Some(1882), None) shouldBe 0.5 +- .01
    AuthorsMatcher.yearsScore(None, None) shouldBe 1.0 +- .01
  }
}
