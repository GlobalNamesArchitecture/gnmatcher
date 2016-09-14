package org
package globalnames

import java.io.{ObjectInputStream, FileInputStream, ObjectOutputStream, FileOutputStream}
import java.util
import java.util.Comparator

import com.github.liblevenshtein.transducer.{Candidate => LCandidate, ITransducer, Algorithm}
import com.github.liblevenshtein.transducer.factory.TransducerBuilder

import scala.collection.JavaConversions._

class Matcher private (transducer: ITransducer[LCandidate]) {
  def transduce(queryTerm: String): Seq[Candidate] = {
    transducer.transduce(queryTerm).toVector.map { (c: LCandidate) =>
      Candidate(c.term, c.distance)
    }
  }

  def dump(dumpPath: String) = {
    val fos = new FileOutputStream(dumpPath)
    val oos = new ObjectOutputStream(fos)
    oos.writeObject(transducer)
    oos.close()
  }
}

object Matcher {
  def restore(dumpPath: String): Matcher = {
    val transducer: ITransducer[LCandidate] = {
      val fileIn = new FileInputStream(dumpPath)
      val in = new ObjectInputStream(fileIn)
      val e = in.readObject().asInstanceOf[ITransducer[LCandidate]]
      in.close()
      fileIn.close()
      e
    }
    new Matcher(transducer)
  }

  def apply(canonicalNames: Seq[String], maxDistance: Int) = {
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
