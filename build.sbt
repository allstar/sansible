name := "sbt-ansible"

version := "0.1.0"

scalaVersion := "2.11.8"

val ansibleRepoUri = settingKey[String]("URI for the ansible-modules-core git repo")
ansibleRepoUri := "git@github.com:ansible/ansible-modules-core.git"

val yamlExtractorScript = settingKey[File]("Path of Ruby YAML extractor script")
yamlExtractorScript := baseDirectory.value / "bin" / "extract_annotations.rb"

val fetchAnsibleSource = taskKey[Seq[File]]("Create some source files")
fetchAnsibleSource := {
  val s = streams.value
  val targetDir = (resourceManaged in Compile).value / "ansible-src"
  if (!targetDir.exists()) {
    s.log.info(s"cloning ansible-modules-core repo from ${ansibleRepoUri.value}")
    Process(s"git clone --depth=1 ${ansibleRepoUri.value} $targetDir") ! s.log
  } else {
    s.log.info(s"ansible-modules-core sources found at $targetDir")
  }
  Seq(targetDir)
}

val extractYamlAnnotations = TaskKey[Seq[File]]("Extract YAML annotations from ansible python modules")
extractYamlAnnotations := {
  val s = streams.value
  val srcDir = fetchAnsibleSource.value.head
  val targetDir = srcDir.getParentFile / "parsed_modules"
  if (!targetDir.exists() || IO.listFiles(targetDir).isEmpty) {
    IO.createDirectory(targetDir)
    s.log.info(s"ruby ${yamlExtractorScript.value} $srcDir $targetDir")
    Process(s"ruby ${yamlExtractorScript.value} $srcDir $targetDir") ! s.log
  }
  Seq(targetDir)
}

val genResources = extractYamlAnnotations dependsOn fetchAnsibleSource

resourceGenerators in Compile += genResources.taskValue

