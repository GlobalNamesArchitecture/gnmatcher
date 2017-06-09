package org.globalnames.matcher

import com.github.liblevenshtein.transducer.factory.TransducerBuilder
import com.github.liblevenshtein.transducer.{Algorithm, ITransducer, Candidate => LCandidate}

import scala.collection.JavaConverters._

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
        val candidates = transducerByStem.transduce(givenWordLower.toLowerCase).asScala.toVector
        candidates.filter { foundWord =>
            val foundWordStems = foundWord.term.split(" ").map { p => LatinStemmer.stemmize(p) }
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

  def apply(canonicalNames: Seq[String]): Matcher = {
    val canonicalNamesTransducerMaxDistance = 1
    val canonicalNamesStemsTransducerMaxDistance = 4
    val dictionary = canonicalNames.map { _.toLowerCase }.sorted.asJava

    val canonicalLowerToFull =
      canonicalNames.foldLeft(Map.empty[String, Vector[String]].withDefaultValue(Vector())) {
        case (mp, name) =>
          val nameLower = name.toLowerCase
          mp + (nameLower -> (name +: mp(nameLower)))
      }

    val canonicalNamesTransducer = new TransducerBuilder()
      .algorithm(Algorithm.STANDARD)
      .defaultMaxDistance(canonicalNamesTransducerMaxDistance)
      .dictionary(dictionary, true)
      .build[LCandidate]()

    val canonicalNamesStemsTransducer = new TransducerBuilder()
      .algorithm(Algorithm.STANDARD)
      .defaultMaxDistance(canonicalNamesStemsTransducerMaxDistance)
      .dictionary(dictionary, true)
      .build[LCandidate]()

    new Matcher(canonicalNamesTransducer, canonicalNamesStemsTransducer, canonicalLowerToFull)
  }

}
