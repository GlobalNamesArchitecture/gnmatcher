import sbt.Keys._

val commonSettings = Seq(
  version := {
    val release = sys.props.isDefinedAt("release")
    val version = "0.1.3"
    if (release) version
    else version + sys.props.get("buildNumber").map { "-" + _ }.getOrElse("") + "-SNAPSHOT"
  },
  scalaVersion := "2.11.12",
  organization in ThisBuild := "org.globalnames",
  homepage := Some(new URL("http://globalnames.org/")),
  description := "Fast Levenshtein Automata matcher for scientific names",
  startYear := Some(2015),
  licenses := Seq("MIT" -> new URL("https://github.com/GlobalNamesArchitecture/gnmatcher/blob/master/LICENSE")),
  resolvers ++= Seq(
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  ),
  javacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-source", "1.8",
    "-target", "1.8",
    "-Xlint:unchecked",
    "-Xlint:deprecation"),
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-language:_",
    "-target:jvm-1.8",
    "-Xlog-reflective-calls"))

val publishingSettings = Seq(
  publishMavenStyle := true,
  useGpg := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else                  Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <scm>
      <url>git@github.com:GlobalNamesArchitecture/gnmatcher.git</url>
      <connection>scm:git:git@github.com:GlobalNamesArchitecture/gnmatcher.git</connection>
    </scm>
      <developers>
        <developer>
          <id>dimus</id>
          <name>Dmitry Mozzherin</name>
        </developer>
        <developer>
          <id>alexander-myltsev</id>
          <name>Alexander Myltsev</name>
          <url>http://myltsev.com</url>
        </developer>
      </developers>)

val noPublishingSettings = Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))))

/////////////////////// DEPENDENCIES /////////////////////////

lazy val versions = new {
  val scalatest = "3.0.5"
  val liblevenshtein = "3.0.0"
  val jython = "2.7.1"
  val scalaz = "7.2.21"
  val pprint = "0.5.3"
  val logback = "1.2.3"
  val scalaLogging = "3.9.0"
}

val liblevenshtein = "com.github.universal-automata"  %  "liblevenshtein"        % versions.liblevenshtein
val jython         = "org.python"                     %  "jython-standalone"     % versions.jython
val scalatest      = "org.scalatest"                  %% "scalatest"             % versions.scalatest % Test
val scalaz         = "org.scalaz"                     %% "scalaz-core"           % versions.scalaz
val pprint         = "com.lihaoyi"                    %% "pprint"                % versions.pprint
val logback        = "ch.qos.logback"                 %  "logback-classic"       % versions.logback
val scalaLogging   = "com.typesafe.scala-logging"     %% "scala-logging"         % versions.scalaLogging

/////////////////////// PROJECTS /////////////////////////

lazy val `gnmatcher-root` = project.in(file("."))
  .aggregate(matcher)
  .settings(noPublishingSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.11.8", "2.12.6")
  )

lazy val matcher = (project in file("./matcher"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(publishingSettings: _*)
  .settings(
    name := "gnmatcher",

    test in assembly := {},

    libraryDependencies ++= Seq(scalaz, pprint, scalaLogging, logback, liblevenshtein, jython,
                                scalatest),

    scalacOptions in Test ++= Seq("-Yrangepos"),

    initialCommands in console := """
      |import org.globalnames._
      |import org.globalnames.matcher._
      """.stripMargin
  )
