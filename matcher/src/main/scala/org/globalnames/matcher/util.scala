package org.globalnames.matcher

import com.BoxOfC.MDAG.MDAG

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.Success

object util {

  implicit class MDAGEnhanced(val mdagFut: Future[MDAG]) extends AnyVal {
    def valueOrEmpty: MDAG = mdagFut.value match {
      case Some(Success(m)) => m
      case _ => new MDAG(Seq())
    }
  }

}
