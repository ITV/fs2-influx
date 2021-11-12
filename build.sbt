import sbt._
import ReleaseTransformations._
import sbt.Keys.name

Global / bloopExportJarClassifiers := Some(Set("sources"))

val commonSettings: Seq[Setting[_]] = Seq(
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/ITV/fs2-influx"),
      "scm:git@github.com:ITV/fs2-influx.git"
    )
  ),
  organization                              := "com.itv",
  organizationName                          := "ITV",
  scalaVersion                              := "2.13.7",
  crossScalaVersions                        := Seq("2.12.15", scalaVersion.value),
  Global / bloopAggregateSourceDependencies := true,
  licenses                                  := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  ThisBuild / publishTo                     := sonatypePublishToBundle.value,
  ThisBuild / pomIncludeRepository          := { _ => false },
  publishMavenStyle                         := true,
  pomExtra :=
    <url>https://github.com/ITV/fs2-influx</url>
      <developers>
        <developer>
          <id>jbwheatley</id>
          <name>Jack Wheatley</name>
          <organization>ITV</organization>
          <organizationUrl>http://www.itv.com</organizationUrl>
        </developer>
      </developers>
)

lazy val root = (project in file("."))
  .aggregate(core, http4s, docs)
  .settings(commonSettings)
  .settings(
    publish / skip := true
  )

lazy val core = project
  .in(file("fs2-influx"))
  .settings(commonSettings)
  .settings(
    name := "fs2-influx",
    libraryDependencies ++= Seq(
      "org.influxdb"   % "influxdb-java"                 % "2.21",
      "co.fs2"        %% "fs2-core"                      % "3.2.2",
      "com.dimafeng"  %% "testcontainers-scala-munit"    % "0.39.11" % Test,
      "com.dimafeng"  %% "testcontainers-scala-influxdb" % "0.39.11" % Test,
      "org.typelevel" %% "munit-cats-effect-3"           % "1.0.6"   % Test
    )
  )

lazy val http4s = project
  .in(file("fs2-influx-http4s"))
  .settings(commonSettings)
  .settings(
    name := "fs2-influx-http4s",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % "0.23.6"
    )
  )
  .dependsOn(core)

lazy val docs = project
  .in(file("fs2-influx-docs"))
  .enablePlugins(MdocPlugin)
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    mdocOut        := (ThisBuild / baseDirectory).value,
    mdocVariables := Map(
      "FS2INFLUX_VERSION" -> version.value
    ),
    releaseProcess := Seq[ReleaseStep](
      ReleasePlugin.autoImport.releaseStepInputTask(MdocPlugin.autoImport.mdoc),
      ReleaseMdocStateTransformations.commitMdoc
    )
  )
  .dependsOn(core)

addCommandAlias("buildFs2Influx", ";clean;+test;mdoc")

releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  //  releaseStepInputTask(MdocPlugin.autoImport.mdoc),
  //  ReleaseMdocStateTransformations.commitMdoc,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
