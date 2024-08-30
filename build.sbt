name := "prometheus-tools"

organization := "com.evolutiongaming"

homepage := Some(url("https://github.com/evolution-gaming/prometheus-tools"))

startYear := Some(2018)

organizationName := "Evolution"

organizationHomepage := Some(url("https://evolution.com"))

crossScalaVersions := Seq("2.13.14", "3.3.3")

scalaVersion := crossScalaVersions.value.head

Compile / scalacOptions ++= Seq("-language:implicitConversions")
Compile / scalacOptions ++= {
  if (scalaBinaryVersion.value == "2.13") {
    Seq(
      "-Xsource:3"
    )
  } else Seq.empty
}

Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings")

publishTo := Some(Resolver.evolutionReleases)

libraryDependencies ++= Seq(
  // executor-tools dependency is not used anymore, left as is so MiMa bincompat report doesn't complain
  // TODO: remove in 2.x
  "com.evolutiongaming" %% "executor-tools"      % "1.0.4",
  "io.prometheus"        % "simpleclient_common" % "0.8.1",
  "org.scalameta"       %% "munit"               % "1.0.0" % Test
)

testFrameworks += new TestFramework("munit.Framework")

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true

ThisBuild / versionScheme := Some("semver-spec")

// Your next release will be binary compatible with the previous one,
// but it may not be source compatible (ie, it will be a minor release).
ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible

//used by evolution-gaming/scala-github-actions
addCommandAlias("check", "all versionPolicyCheck Compile/doc scalafmtCheckAll scalafmtSbtCheck")

addCommandAlias("fmtAll", "all scalafmtAll scalafmtSbt")
