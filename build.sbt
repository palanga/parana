name := "parana"

val PARANA_VERSION = "0.4.3"

val MAIN_SCALA = "2.13.4"
val ALL_SCALA  = Seq(MAIN_SCALA)

val ZIO_CASSANDRA_VERSION = "0.3.0"

val ZIO_JSON_VERSION = "0.1.5"

val ZIO_VERSION = "1.0.7"

inThisBuild(
  List(
    organization := "dev.palanga",
    homepage := Some(url("https://github.com/palanga/parana")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    Test / parallelExecution := false,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/palanga/parana/"),
        "scm:git:git@github.com:palanga/parana.git",
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
    .settings(publish / skip := true)
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
      name := "parana-core",
      version := PARANA_VERSION,
      Test / fork := true,
      run / fork := true,
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
      name := "parana-journal-cassandra",
      version := PARANA_VERSION,
      Test / fork := true,
      run / fork := true,
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
      name := "parana-journal-cassandra-json",
      version := PARANA_VERSION,
      Test / fork := true,
      run / fork := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-json"     % ZIO_JSON_VERSION,
        "dev.zio" %% "zio-test"     % ZIO_VERSION % "test",
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION % "test",
      ),
    )
    .dependsOn(
      journal_cassandra,
      core % "test->test",
    )

lazy val examples =
  (project in file("examples"))
    .settings(commonSettings)
    .settings(
      name := "examples",
      publish / skip := true,
      Test / fork := true,
      run / fork := true,
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
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:patvars,-implicits",
      "-Ywarn-value-discard",
    ),
  )
