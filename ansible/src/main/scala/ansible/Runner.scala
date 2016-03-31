package ansible

import better.files.File
import IniEncode._
import IniEncoders._
import scala.sys.process.{Process, ProcessIO}
import scala.io.Source

object Runner {
  def runPlaybook(inv: Inventory)(pb: Playbook): Unit = {
    val invFile = File.newTemporaryFile("ansible-inventory")
    val pbFile  = File.newTemporaryFile("ansible-playbook", "yml")
    val pio = new ProcessIO(
      _ => (),
      out  => Source.fromInputStream(out).getLines.foreach(println),
      err => Source.fromInputStream(err).getLines.foreach(System.err.println)
    )


    invFile.write(inv.iniEncode)
    pbFile.write(YAML.fromPlaybook(pb))

    println(s"ansible-playbook -i ${invFile.path} ${pbFile.path}")

    Process(
      s"ansible-playbook -vvv -i ${invFile.path} ${pbFile.path}",
      cwd = None,
      Seq("ANSIBLE_FORCE_COLOR" -> "true"): _*
    ).run(pio)
  }
}
