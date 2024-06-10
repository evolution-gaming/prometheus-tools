name := "prometheus-tools"

organization := "com.evolutiongaming"

homepage := Some(new URL("http://github.com/evolution-gaming/prometheus-tools"))

startYear := Some(2018)

organizationName := "Evolution"

organizationHomepage := Some(url("http://evolution.com"))

crossScalaVersions := Seq("2.13.7", "2.12.15", "3.3.3")

scalaVersion := crossScalaVersions.value.head

Compile / scalacOptions ++= Seq("-language:implicitConversions")

Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings")

publishTo := Some(Resolver.evolutionReleases)

libraryDependencies ++= Seq(
  "com.evolutiongaming" %% "executor-tools"     % "1.0.4",
  "io.prometheus"       % "simpleclient_common" % "0.8.1",
  "org.scalameta"       %% "munit"              % "0.7.29" % Test
)

testFrameworks += new TestFramework("munit.Framework")

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true

ThisBuild / versionScheme := Some("semver-spec")

addCommandAlias("check", "all scalafmtCheckAll scalafmtSbtCheck")
