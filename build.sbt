name := "zio-event-sourcing"

val ZIO_EVENT_SOURCING_VERSION = "0.1.2"

val MAIN_SCALA = "2.13.4"
val ALL_SCALA  = Seq(MAIN_SCALA)

val ACONCAGUA_VERSION = "0.0.1"

val CALIBAN_VERSION = "0.9.4"

val ZIO_CASSANDRA_VERSION = "0.2.0"

val ZIO_JSON_VERSION = "0.1"

val ZIO_VERSION = "1.0.5"

inThisBuild(
  List(
    organization := "dev.palanga",
    homepage := Some(url("https://github.com/palanga/zio-event-sourcing")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    parallelExecution in Test := false,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/palanga/zio-event-sourcing/"),
        "scm:git:git@github.com:palanga/zio-event-sourcing.git",
      )
    ),
    developers := List(
      Developer(
        "palanga",
        "Andrés González",
        "a.gonzalez.terres@gmail.com",
        url("https://github.com/palanga"),
      )
    ),
    publishTo := Some("Artifactory Realm" at "https://palanga.jfrog.io/artifactory/maven/"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root =
  (project in file("."))
    .settings(skip in publish := true)
    .aggregate(
      core,
      journal_cassandra,
      journal_cassandra_json,
      examples,
    )

lazy val core =
  (project in file("core"))
    .settings(commonSettings)
    .settings(
      name := "zio-event-sourcing-core",
      version := ZIO_EVENT_SOURCING_VERSION,
      fork in Test := true,
      fork in run := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio"          % ZIO_VERSION,
        "dev.zio" %% "zio-streams"  % ZIO_VERSION,
        "dev.zio" %% "zio-test"     % ZIO_VERSION % "test",
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION % "test",
      ),
    )

lazy val journal_cassandra =
  (project in file("journal/cassandra"))
    .settings(commonSettings)
    .settings(
      name := "zio-event-sourcing-journal-cassandra",
      version := ZIO_EVENT_SOURCING_VERSION,
      fork in Test := true,
      fork in run := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.palanga" %% "zio-cassandra" % ZIO_CASSANDRA_VERSION
      ),
    )
    .dependsOn(core)

lazy val journal_cassandra_json =
  (project in file("journal/cassandra/json"))
    .settings(commonSettings)
    .settings(
      name := "zio-event-sourcing-journal-cassandra-json",
      version := ZIO_EVENT_SOURCING_VERSION,
      fork in Test := true,
      fork in run := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-json" % ZIO_JSON_VERSION
      ),
    )
    .dependsOn(journal_cassandra)

lazy val examples =
  (project in file("examples"))
    .settings(commonSettings)
    .settings(
      name := "examples",
      skip in publish := true,
      fork in Test := true,
      fork in run := true,
      libraryDependencies ++= Seq(
        "dev.palanga"   %% "aconcagua"       % ACONCAGUA_VERSION,
        "ch.qos.logback" % "logback-classic" % "1.2.3",
      ),
    )
    .dependsOn(
      core,
      journal_cassandra_json,
    )

val commonSettings =
  Def.settings(
    scalaVersion := MAIN_SCALA,
    crossScalaVersions := ALL_SCALA,
    libraryDependencies += compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    resolvers += "Artifactory" at "https://palanga.jfrog.io/artifactory/maven/",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-explaintypes",
      "-Yrangepos",
      "-feature",
      "-language:higherKinds",
      "-language:existentials",
      "-unchecked",
      "-Xlint:_,-type-parameter-shadow",
      //    "-Xfatal-warnings",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:patvars,-implicits",
      "-Ywarn-value-discard",
//      "-Ymacro-annotations",
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        Seq(
          "-Xsource:2.13",
          "-Yno-adapted-args",
          "-Ypartial-unification",
          "-Ywarn-extra-implicit",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-opt-inline-from:<source>",
          "-opt-warnings",
          "-opt:l:inline",
        )
      case _             => Nil
    }),
//    scalacOptions in Test --= Seq("-Xfatal-warnings"),
  )
