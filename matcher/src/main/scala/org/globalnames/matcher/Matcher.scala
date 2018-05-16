package org.globalnames.matcher

import java.util.Properties

import org.python.core._
import org.python.util.PythonInterpreter

import scala.io.Source

class Matcher private(finderInstance: PyInstance) {
  def findMatches(verbatim: String, dataSources: Set[Int]): Vector[Candidate] = {
    val dataSourcesArr = dataSources.map { ds => Py.newInteger(ds).asInstanceOf[PyObject] }.toArray
    val dataSourcesPy = new PySet(dataSourcesArr)

    val candidates: Array[String] =
      finderInstance.invoke(
        "find_all_matches",
        new PyString(verbatim),
        dataSourcesPy
      ).__tojava__(classOf[Array[String]]).asInstanceOf[Array[String]]

    val result = candidates.map { cand => Candidate(cand, 1, None, None) }.toVector
    result
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
    val props = new Properties()
    props.put("python.home", "/home/amyltsev/.ivy2/cache/org.python/jython-standalone/jars/")
    // Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
    props.put("python.console.encoding", "UTF-8")
    //don't respect java accessibility, so that we can access protected members on subclasses
    props.put("python.security.respectJavaAccessibility", "false")
    props.put("python.import.site", "false")

    PythonInterpreter.initialize(System.getProperties, props, Array[String]())
    val interpreter = new PythonInterpreter()

    val filePy =
      Source.fromURL(getClass.getClassLoader.getResource("levenshtein_py/automata.py"))
        .getLines.toVector.mkString("\n")
    interpreter.exec(filePy)

    val wordDatasources = new PyDictionary()
    for ((canonicalName, dataSources) <- canonicalNames) {
      try {
        val psPy = Py.newStringOrUnicode(canonicalName)
        val dsPyInts = dataSources.map { ds => Py.newInteger(ds).asInstanceOf[PyObject]}.toArray
        val dsPy = new PySet(dsPyInts)
        wordDatasources.__setitem__(psPy, dsPy)
      } catch {
        case ex: Exception => ()
      }
    }

    val finderInstance: PyInstance =
      interpreter.get("Finder").__call__(wordDatasources).asInstanceOf[PyInstance]

    new Matcher(finderInstance)
  }

}
