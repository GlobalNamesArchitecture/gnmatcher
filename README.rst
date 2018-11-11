Global Names Matcher
====================

.. image:: https://circleci.com/gh/GlobalNamesArchitecture/gnmatcher.svg?style=svg
    :target: https://circleci.com/gh/GlobalNamesArchitecture/gnmatcher

Global Names Matcher or ``gnmatcher`` is a Scala 2.10.3+ library for very fast
fuzzy matching of a query string against given set of strings.

Installation
------------

The artifacts for ``gnmatcher`` live on `Maven
Central <http://search.maven.org/#search%7Cga%7C1%7Cgnmatcher>`_.

Insert SBT line as follows to install the dependency:

.. code:: Scala

    libraryDependencies += "org.globalnames" %% "gnmatcher" % "0.1.0"

Corresponding maven code:

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

Matching
--------

``gnmatcher`` implements sophisticated heuristic algorithms to match semantical parts of
scientific biological names as follows:

- authors match answers to a question: how similar the authors string ``Linnaeus, Muller 1767``
  to the ``Muller and Linnaeus``?

Authors Matching
~~~~~~~~~~~~~~~~

The entire algorithm is ported from `Ruby implementation
<https://github.com/GlobalNamesArchitecture/taxamatch_rb/blob/master/lib/taxamatch_rb/authmatch.rb>`_
developed by Patrick Leary of uBio and EOL fame. To find out the answer to the question above, run
the code as follows:

.. code:: Scala

    $ sbt matcher/console
    scala> import org.globalnames._
    scala> AuthorsMatcher.score(Seq(Author("Linnaeus"), Author("Muller")), Some(1767),
         |                      Seq(Author("Muller"), Author("Linnaeus")), None)
    res0: Double = 0.5


Contributors
------------

+ Alexander Myltsev `http://myltsev.com <http://myltsev.com>`_ `alexander-myltsev@github <https://github.com/alexander-myltsev>`_
+ Dmitry Mozzherin `dimus@github <https://github.com/dimus>`_

License
-------

Released under `MIT license </LICENSE>`_
