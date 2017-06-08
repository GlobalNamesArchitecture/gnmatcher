package org.globalnames.matcher

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util
import java.util.Comparator

import com.github.liblevenshtein.transducer.factory.TransducerBuilder
import com.github.liblevenshtein.transducer.{Algorithm, ITransducer, Candidate => LCandidate}

import scala.collection.JavaConversions._

class Matcher private (transducer: ITransducer[LCandidate]) {
  def transduce(queryTerm: String): Seq[Candidate] = {
    transducer.transduce(queryTerm).toVector.map { (c: LCandidate) =>
      Candidate(c.term, c.distance)
    }
  }
}

object Matcher {
  def apply(canonicalNames: Seq[String], maxDistance: Int): Matcher = {
    val transducer: ITransducer[LCandidate] = {
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
    new Matcher(transducer)
  }
}
