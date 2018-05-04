package org.globalnames.matcher

import com.github.liblevenshtein.transducer.factory.TransducerBuilder
import com.github.liblevenshtein.transducer.{Algorithm, ITransducer, Candidate => LCandidate}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class Matcher private(transducerByVerbatim: ITransducer[LCandidate],
                      transducerByStem: ITransducer[LCandidate],
                      canonicalNamesDataSources: Map[String, Set[Int]],
                      canonicalStemsLowerToFullMap: Map[String, Vector[String]],
                      canonicalVerbatimLowerToFullMap: Map[String, Vector[String]]) {

  def findMatches(verbatim: String, dataSources: Set[Int]): Vector[Candidate] = {
    val givenWordSanitized = Matcher.sanitizeWord(verbatim)

    if (givenWordSanitized.isEmpty) {
      Vector.empty
    } else {
      val givenWordLower = givenWordSanitized.toLowerCase

      if (givenWordLower.indexOf(' ') == -1) {
        for {
          wordFull <- canonicalVerbatimLowerToFullMap(givenWordLower)
          wordDataSourceId <- canonicalNamesDataSources.getOrElse(wordFull, Set())
          if dataSources.isEmpty || dataSources.contains(wordDataSourceId)
        } yield Candidate(wordFull, wordDataSourceId, None, None)
      } else {
        val givenWordLowerStemmized = Matcher.stemmize(givenWordLower)
        val candidatesByStem = transducerByStem.transduce(givenWordLowerStemmized).asScala.toVector
        val candidatesByStemFiltered = for {
          candidate <- candidatesByStem
          wordFull <- canonicalStemsLowerToFullMap(candidate.term)
          wordDataSourceId <- canonicalNamesDataSources.getOrElse(wordFull, Set())
          if dataSources.isEmpty || dataSources.contains(wordDataSourceId)
        } yield Candidate(wordFull, wordDataSourceId, None, Some(wordDataSourceId))

        val candidatesByVerbatim = transducerByVerbatim.transduce(givenWordLower).asScala.toVector
        val candidatesByVerbatimFiltered = for {
          candidate <- candidatesByVerbatim
          wordFull <- canonicalStemsLowerToFullMap(candidate.term)
          wordDataSourceId <- canonicalNamesDataSources.getOrElse(wordFull, Set())
          if dataSources.isEmpty || dataSources.contains(wordDataSourceId)
        } yield Candidate(wordFull, wordDataSourceId, Some(candidate.distance), None)

        candidatesByStemFiltered ++ candidatesByVerbatimFiltered
      }
    }
  }

}

object Matcher {

  def sanitizeWord(word: String): String = {
    word.trim.replaceAll("\\s+", " ")
  }

  def stemmize(word: String): String = {
    val nameLowerParts = word.toLowerCase.split(" ")
    nameLowerParts.map { part => LatinStemmer.stemmize(part).mappedStem }.mkString(" ")
  }

  def apply(canonicalNames: Map[String, Set[Int]]): Matcher = {
    val canonicalNamesTransducerMaxDistance = 2
    val canonicalNamesStemsTransducerMaxDistance = 2

    val canonicalNamesSanitized = canonicalNames.keys.map { sanitizeWord }

    val canonicalVerbatimLowerToFullMap =
      canonicalNamesSanitized.foldLeft(Map.empty[String, Vector[String]].withDefaultValue(Vector())) {
        case (mp, name) =>
          val nameLower = name.toLowerCase
          mp + (nameLower -> (name +: mp(nameLower)))
      }

    val canonicalStemsLowerToFullMap =
      canonicalNamesSanitized.foldLeft(Map.empty[String, Vector[String]].withDefaultValue(Vector())) {
        case (mp, name) =>
          val nameStemmized = stemmize(name)
          mp + (nameStemmized -> (name +: mp(nameStemmized)))
      }

    val canonicalNamesTransducerFut = Future {
      new TransducerBuilder()
        .algorithm(Algorithm.STANDARD)
        .defaultMaxDistance(canonicalNamesTransducerMaxDistance)
        .dictionary(canonicalVerbatimLowerToFullMap.keys.toVector.sorted.asJava, true)
        .build[LCandidate]()
    }

    val canonicalNamesStemsTransducerFut = Future {
      new TransducerBuilder()
        .algorithm(Algorithm.STANDARD)
        .defaultMaxDistance(canonicalNamesStemsTransducerMaxDistance)
        .dictionary(canonicalStemsLowerToFullMap.keys.toVector.sorted.asJava, true)
        .build[LCandidate]()
    }

    val matcherFut =
      for {
        cnt <- canonicalNamesTransducerFut
        cnst <- canonicalNamesStemsTransducerFut
      } yield new Matcher(cnt, cnst, canonicalNames,
                          canonicalStemsLowerToFullMap, canonicalVerbatimLowerToFullMap)
    Await.result(matcherFut, 60.minutes)
  }

}
