val scalaV = "2.11.7"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaV,
  "com.github.pathikrit" %% "better-files" % "2.15.0",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

val ansibleRepoUri = settingKey[String]("URI for the ansible-modules-core git repo")
ansibleRepoUri := "git@github.com:ansible/ansible-modules-core.git"

val yamlExtractorScript = settingKey[File]("Path of Ruby YAML extractor script")
yamlExtractorScript := baseDirectory.value / "bin" / "extract_annotations.rb"

val fetchAnsibleSource = taskKey[File]("Create some source files")
fetchAnsibleSource := {
  val s = streams.value
  val targetDir = (resourceManaged in Compile).value / "ansible-src"
  if (!targetDir.exists()) {
    s.log.info(s"cloning ansible-modules-core repo from ${ansibleRepoUri.value}")
    Process(s"git clone --depth=1 ${ansibleRepoUri.value} $targetDir") ! s.log
  } else {
    s.log.info(s"ansible-modules-core sources found at $targetDir")
  }
  targetDir
}

val extractYamlAnnotations = TaskKey[Seq[File]]("Extract YAML annotations from ansible python modules")
extractYamlAnnotations := {
  val s = streams.value
  val srcDir = fetchAnsibleSource.value
  val tmpDir = file(System.getProperty("java.io.tmpdir"))

  val targetDir = tmpDir / "parsed_modules"
  if (!targetDir.exists() || IO.listFiles(targetDir).isEmpty) {
    IO.createDirectory(targetDir)
    s.log.info(s"ruby ${yamlExtractorScript.value} $srcDir $targetDir")
    Process(s"ruby ${yamlExtractorScript.value} $srcDir $targetDir") ! s.log
  }
  Seq(targetDir)
}

resourceGenerators in Compile += (extractYamlAnnotations dependsOn fetchAnsibleSource).taskValue
