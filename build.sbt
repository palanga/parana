name := "parana"

val MAIN_SCALA            = "3.1.3"
val ALL_SCALA             = Seq(MAIN_SCALA)
val ZIO_CASSANDRA_VERSION = "0.9.0"
val ZIO_JSON_VERSION      = "0.3.0-RC11"
val ZIO_VERSION           = "2.0.1"

inThisBuild(
  List(
    organization           := "io.github.palanga",
    homepage               := Some(url("https://github.com/palanga/zio-cassandra")),
    licenses               := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    resolvers += "Sonatype OSS Releases" at "https://s01.oss.sonatype.org/content/repositories/releases",
    resolvers += "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots",
    developers             := List(
      Developer(
        "palanga",
        "Andrés González",
        "a.gonzalez.terres@gmail.com",
        url("https://github.com/palanga/"),
      )
    ),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root =
  (project in file("."))
    .settings(publish / skip := true)
    .aggregate(
      core,
      core_local,
      core_remote,
      journal,
      journal_cassandra,
      journal_cassandra_json,
      examples,
    )

lazy val core =
  (project in file("core"))
    .settings(
      name           := "parana",
      description    := "An event sourcing library on top of ZIO",
      publish / skip := true,
      Test / fork    := true,
      run / fork     := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")), // TODO remove
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio"          % ZIO_VERSION,
        "dev.zio" %% "zio-streams"  % ZIO_VERSION,
        "dev.zio" %% "zio-test"     % ZIO_VERSION % "test", // TODO remove
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION % "test", // TODO remove
      ),
      commonSettings,
    )

lazy val core_local =
  (project in file("core") / "local")
    .settings(
      name           := "parana-local",
      description    := "An event sourcing library on top of ZIO",
      Test / fork    := true,
      run / fork     := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test"     % ZIO_VERSION % "test",
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION % "test",
      ),
      commonSettings,
    ).dependsOn(core)

lazy val core_remote =
  (project in file("core") / "remote")
    .settings(
      name           := "parana-remote",
      description    := "An event sourcing library on top of ZIO",
      Test / fork    := true,
      run / fork     := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test"     % ZIO_VERSION % "test",
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION % "test",
      ),
      commonSettings,
    ).dependsOn(core)

lazy val journal =
  (project in file("journal"))
    .settings(
      name           := "parana-journal",
      description    := "An event sourcing library on top of ZIO",
      Test / fork    := true,
      run / fork     := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test"     % ZIO_VERSION % "test",
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION % "test",
      ),
      commonSettings,
    ).dependsOn(core)

lazy val journal_cassandra =
  (project in file("journal/cassandra"))
    .settings(
      name           := "parana-journal-cassandra",
      Test / fork    := true,
      run / fork     := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "io.github.palanga" %% "zio-cassandra" % ZIO_CASSANDRA_VERSION
      ),
      commonSettings,
    )
    .dependsOn(journal)

lazy val journal_cassandra_json =
  (project in file("journal/cassandra/json"))
    .settings(
      name           := "parana-journal-cassandra-json",
      Test / fork    := true,
      run / fork     := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-json"     % ZIO_JSON_VERSION,
        "dev.zio" %% "zio-test"     % ZIO_VERSION % "test",
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION % "test",
      ),
      scalacOptions += "-Yretain-trees",
      commonSettings,
    )
    .dependsOn(
      journal_cassandra,
      core % "test->test",
    )

lazy val examples =
  (project in file("examples"))
    .settings(
      name           := "examples",
      publish / skip := true,
      Test / fork    := true,
      run / fork     := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test" % ZIO_VERSION,
        "dev.zio" %% "zio-test-sbt" % ZIO_VERSION,
      ),
      commonSettings,
    )
    .dependsOn(
      core_local,
    )

val commonSettings = Def.settings(
  scalaVersion       := MAIN_SCALA,
  crossScalaVersions := ALL_SCALA,
  versionScheme      := Some("strict"),
  scalacOptions ++= Seq(
    "-source:future",
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-explaintypes",
    "-feature",
    "-language:higherKinds",
    "-language:existentials",
    "-unchecked",
  ),
)
