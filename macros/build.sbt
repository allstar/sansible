val scalaV = "2.11.7"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaV,
  "com.github.pathikrit" %% "better-files" % "2.15.0",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)

val ansibleModulesRepoUri = settingKey[String]("URI for the ansible-modules-core git repo")
ansibleModulesRepoUri := "https://github.com/ansible/ansible-modules-core.git"

val ansibleOverridesRepoUri = settingKey[String]("URI for the ansible git repo containing yaml doc overriedes")
ansibleOverridesRepoUri := "https://github.com/ansible/ansible.git"

val yamlExtractorScript = settingKey[File]("Path of Ruby YAML extractor script")
yamlExtractorScript := baseDirectory.value / "bin" / "extract_annotations.rb"

val fetchAnsibleSource = taskKey[(File, File)]("Clone ansible sources")
fetchAnsibleSource := {
  val s = streams.value
  val sourceDir = (resourceManaged in Compile).value / "ansible-src"
  val overridesDir = (resourceManaged in Compile).value / "ansible-overrides"

  if (!sourceDir.exists()) {
    s.log.info(s"cloning ansible-modules-core repo from ${ansibleModulesRepoUri.value}")
    Process(s"git clone --depth=1 ${ansibleModulesRepoUri.value} $sourceDir") ! s.log
  } else {
    s.log.info(s"ansible-modules-core sources found at $sourceDir")
  }

  if (!overridesDir.exists()) {
    s.log.info(s"cloning ansible overrides repo from ${ansibleOverridesRepoUri.value}")
    Process(s"git clone --depth=1 ${ansibleOverridesRepoUri.value} $overridesDir") ! s.log
  } else {
    s.log.info(s"ansible overrides found at $overridesDir")
  }

  (sourceDir, overridesDir)
}

val extractYamlAnnotations = TaskKey[Seq[File]]("Extract YAML annotations from ansible python modules")
extractYamlAnnotations := {
  val s = streams.value
  val (srcDir, overridesDir) = fetchAnsibleSource.value
  val tmpDir = file(System.getProperty("java.io.tmpdir"))

  val targetDir = tmpDir / "parsed_modules"
  if (!targetDir.exists() || IO.listFiles(targetDir).isEmpty) {
    IO.createDirectory(targetDir)
    s.log.info(s"ruby ${yamlExtractorScript.value} $srcDir $overridesDir $targetDir")
    Process(s"ruby ${yamlExtractorScript.value} $srcDir $overridesDir $targetDir") ! s.log
  }
  Seq(targetDir)
}

resourceGenerators in Compile += (extractYamlAnnotations dependsOn fetchAnsibleSource).taskValue
