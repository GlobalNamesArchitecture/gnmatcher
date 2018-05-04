package org.globalnames.matcher

case class Candidate(term: String, dataSourceId: Int,
                     verbatimEditDistance: Option[Int],
                     stemEditDistance: Option[Int])
