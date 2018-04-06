package org.globalnames.matcher

import com.github.liblevenshtein.transducer.factory.TransducerBuilder
import com.github.liblevenshtein.transducer.{Algorithm, ITransducer, Candidate => LCandidate}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import akka.http.impl.util._

class Matcher private(transducerByWord: ITransducer[LCandidate],
                      transducerByStem: ITransducer[LCandidate],
                      canonicalLowerToFull: Map[String, Vector[String]]) {

  def findMatches(word: String): Vector[Candidate] = {
    val givenWordLower = word.toLowerCase
    val givenWordPartsLower = givenWordLower.split("\\s+")
    val candidates = transducerByWord.transduce(givenWordLower).asScala.toVector
    val appropriateCandidates =
      if (candidates.nonEmpty) candidates
      else {
        val candidates = transducerByStem.transduce(givenWordLower).asScala.toVector
        candidates.filter { foundWord =>
          val foundWordStems = foundWord.term.fastSplit(' ').map { p => LatinStemmer.stemmize(p) }
          foundWordStems.zip(givenWordPartsLower).forall { case (foundWordStem, givenWordPart) =>
            givenWordPart.startsWith(foundWordStem.originalStem) ||
              givenWordLower.startsWith(foundWordStem.mappedStem)
          }
        }
      }
    appropriateCandidates.flatMap { cand =>
      canonicalLowerToFull(cand.term).map { full => Candidate(full, cand.distance) }
    }
  }

}

object Matcher {

  def apply(canonicalNames: Seq[String],
            canonicalNamesTransducerMaxDistance: Int,
            canonicalNamesStemsTransducerMaxDistance: Int): Matcher = {
    val dictionary = canonicalNames.map { _.toLowerCase }.sorted.asJava

    val canonicalLowerToFull =
      canonicalNames.foldLeft(Map.empty[String, Vector[String]].withDefaultValue(Vector())) {
        case (mp, name) =>
          val nameLower = name.toLowerCase
          mp + (nameLower -> (name +: mp(nameLower)))
      }

    val canonicalNamesTransducerFut = Future {
      new TransducerBuilder()
        .algorithm(Algorithm.STANDARD)
        .defaultMaxDistance(canonicalNamesTransducerMaxDistance)
        .dictionary(dictionary, true)
        .build[LCandidate]()
    }

    val canonicalNamesStemsTransducerFut = Future {
      new TransducerBuilder()
        .algorithm(Algorithm.STANDARD)
        .defaultMaxDistance(canonicalNamesStemsTransducerMaxDistance)
        .dictionary(dictionary, true)
        .build[LCandidate]()
    }

    val matcherFut =
      for {
        cnt <- canonicalNamesTransducerFut
        cnst <- canonicalNamesStemsTransducerFut
      } yield new Matcher(cnt, cnst, canonicalLowerToFull)
    Await.result(matcherFut, 30.minutes)
  }

}
