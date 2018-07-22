package org.globalnames.matcher

case class Candidate(term: String,
                     stem: String,
                     dataSourceIds: Set[Int],
                     verbatimEditDistance: Option[Int],
                     stemEditDistance: Option[Int])
