name := "prometheus-tools"

organization := "com.evolutiongaming"

homepage := Some(new URL("http://github.com/evolution-gaming/prometheus-tools"))

startYear := Some(2018)

organizationName := "Evolution Gaming"

organizationHomepage := Some(url("http://evolutiongaming.com"))

bintrayOrganization := Some("evolutiongaming")

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.12.12", "2.13.3")

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
  case Some((2, n)) if n >= 13 => Seq.empty
  case _                       => Seq("-Yno-adapted-args", "-Xfuture")
})

scalacOptions in (Compile, doc) ++= Seq("-groups", "-implicits", "-no-link-warnings")

resolvers += Resolver.bintrayRepo("evolutiongaming", "maven")

libraryDependencies ++= Seq(
  "com.evolutiongaming" %% "executor-tools"     % "1.0.2",
  "io.prometheus"       % "simpleclient_common" % "0.0.26",
  "org.scalameta"       %% "munit"              % "0.7.12" % Test
)

testFrameworks += new TestFramework("munit.Framework")

licenses := Seq(("MIT", url("https://opensource.org/licenses/MIT")))

releaseCrossBuild := true
