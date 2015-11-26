package org
package globalnames

import java.util
import java.util.Comparator

import com.github.dylon.liblevenshtein.levenshtein.{Algorithm, Candidate => LCandidate}
import com.github.dylon.liblevenshtein.levenshtein.factory.TransducerBuilder

import scala.collection.JavaConversions._

class Matcher(canonicalNames: Seq[String], maxDistance: Int) {
  private val transducer = {
    val tb = new TransducerBuilder()
      .algorithm(Algorithm.MERGE_AND_SPLIT)
      .defaultMaxDistance(maxDistance)
    val dict = new util.ArrayList[String](canonicalNames)
    dict.sort(new Comparator[String] {
      override def compare(o1: String, o2: String): Int = o1.compareTo(o2)
    })
    tb.dictionary(dict, true)
      .build()
  }

  def transduce(queryTerm: String): Seq[Candidate] =
    transducer.transduce(queryTerm).toVector.map { (c: LCandidate) =>
      Candidate(c.term, c.distance)
    }
}
