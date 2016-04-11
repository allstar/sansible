name := "sansible"

organization := "com.citycontext"

version := "0.1.2-SNAPSHOT"

description := "Type-safe Scala DSL for working with Ansible playbooks and inventories"

publishMavenStyle := true

libraryDependencies ++= Seq(
  "ch.qos.logback" %  "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)

homepage := Some(url("http://github.com/citycontext/sansible"))

licenses := Seq(
  "MIT" -> url("https://opensource.org/licenses/MIT")
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
    <scm>
      <url>git@github.com:citycontext/sansible.git</url>
      <connection>scm:git:git@github.com:citycontext/sansible.git</connection>
    </scm>
    <developers>
      <developer>
        <id>afiore</id>
        <name>Andrea Fiore</name>
      </developer>
      <developer>
        <id>tfranquelin</id>
        <name>Thomas Franquelin</name>
      </developer>
    </developers>)

