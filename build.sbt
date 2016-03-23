name := "sbt-ansible"

version := "0.1.0"

val scalaV = "2.11.7"

scalaVersion := scalaV

lazy val root =
  (project in file(".")).
    aggregate(macros, ansible)

lazy val commonSettings = Seq(
  scalaVersion := scalaV,
  scalacOptions += "-deprecation",
  version := "1.0-SNAPSHOT",
  organization := "com.citycontext",
  libraryDependencies ++= Seq(
    "io.argonaut" %% "argonaut" % "6.1",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  ),
  addCompilerPlugin(
    "org.scalamacros" % "paradise_2.11.7" % "2.1.0"
  )
)

lazy val macros = (project in file("macros")).
  settings(commonSettings: _*)

lazy val ansible = (project in file("ansible")).
  settings(commonSettings: _*).
  dependsOn(macros)