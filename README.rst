Global Names Matcher
====================

Global Names Matcher or ``gnmatcher`` is a Scala 2.10.3+ library for very fast
fuzzy matching of a query string against given set of strings.

Dependency Declaration for Java or Scala
----------------------------------------

The artifacts for ``gnmatcher`` live on `Maven
Central <http://search.maven.org/#search%7Cga%7C1%7Cgnmatcher>`_ and can
be set as a dependency in following ways:

SBT:

.. code:: Scala

    libraryDependencies += "org.globalnames" %% "gnmatcher" % "0.1.0"

Maven:

.. code:: xml

    <dependency>
        <groupId>org.globalnames</groupId>
        <artifactId>gnmatcher_2.11</artifactId>
        <version>0.1.0</version>
    </dependency>

    <dependency>
        <groupId>org.globalnames</groupId>
        <artifactId>gnmatcher_2.10</artifactId>
        <version>0.1.0</version>
    </dependency>

Fuzzy Matching
--------------

To match input sequence against query run code as follows:

.. code:: Scala

    $ sbt matcher/console
    console> import org.globalnames.Matcher
    console> val matcher = Matcher(Seq("Abdf", "Abce", "Dddd"), maxDistance = 2)
    console> matcher.transduce("Abc")
    res0: Seq[org.globalnames.Candidate] = Vector(Candidate(Abce,1), Candidate(Abdf,1))

Result contains only `Candidates` edit distance with merges and splits is not greater
than `maxDistance`.

Dump and Restore
----------------

Fuzzy matching is very fast. It is theoretically proven to be constant time of
query string. But building inner data structures for input string might be long.
To avoid rebuilding of `Matcher` it is useful to dump and restore it from file
as follows:

.. code:: Scala

    $ sbt matcher/console
    console> import org.globalnames.Matcher
    console> val matcher = Matcher(Seq("Abdf", "Abce", "Dddd"), maxDistance = 2)
    console> matcher.dump(dumpPath = "matcher.ser")
    console> val matcherRestored = Matcher.restore(dumpPath = "matcher.ser")
    console> matcherRestored.transduce("Abc")
    res0: Seq[org.globalnames.Candidate] = Vector(Candidate(Abce,1), Candidate(Abdf,1))

Contributors
------------

+ Alexander Myltsev `http://myltsev.name <http://myltsev.name>`_ `alexander-myltsev@github <https://github.com/alexander-myltsev>`_
+ Dmitry Mozzherin `dimus@github <https://github.com/dimus>`_

License
-------

Released under `MIT license </LICENSE>`_
