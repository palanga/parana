name := "zio-event-sourcing"

val zioEventSourcingVersion = "0.0.0"

val mainScala = "2.13.4"
val allScala  = Seq(mainScala)

val aconcaguaVersion    = "0.0.1"
val calibanVersion      = "0.9.4"
val zioCassandraVersion = "0.0.2"
val zioVersion          = "1.0.3"

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
      examples,
    )

lazy val core =
  (project in file("core"))
    .settings(commonSettings)
    .settings(
      name := "zio-event-sourcing",
      version := zioEventSourcingVersion,
      fork in Test := true,
      fork in run := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio"          % zioVersion,
        "dev.zio" %% "zio-streams"  % zioVersion,
        "dev.zio" %% "zio-test"     % zioVersion % "test",
        "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      ),
    )

lazy val examples =
  (project in file("examples"))
    .settings(commonSettings)
    .settings(
      name := "zio-event-sourcing-examples",
      skip in publish := true,
      fork in Test := true,
      fork in run := true,
      libraryDependencies ++= Seq(
        "dev.palanga"   %% "aconcagua"       % aconcaguaVersion,
        "dev.palanga"   %% "zio-cassandra"   % zioCassandraVersion,
        "ch.qos.logback" % "logback-classic" % "1.2.3",
      ),
    )
    .dependsOn(
      core
    )

val commonSettings =
  Def.settings(
    scalaVersion := mainScala,
    crossScalaVersions := allScala,
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
