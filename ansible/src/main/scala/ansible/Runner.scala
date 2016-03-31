package ansible

import better.files.File
import IniEncode._
import IniEncoders._
import scala.sys.process.{Process, ProcessIO}
import scala.io.Source

object Runner {
  def runPlaybook(inv: Inventory)(pb: Playbook, opts: Option[String] = None): Unit = {
    val invFile = File.newTemporaryFile("ansible-inventory")
    val pbFile  = File.newTemporaryFile("ansible-playbook", ".yml")
    val pio = new ProcessIO(_ => (),
      out  => Source.fromInputStream(out).getLines.foreach(println),
      err => Source.fromInputStream(err).getLines.foreach(System.err.println)
    )
    val cmd = s"ansible-playbook ${opts.getOrElse("")} -i ${invFile.path} ${pbFile.path}"
    val env = Seq("ANSIBLE_FORCE_COLOR" -> "true")

    invFile.write(inv.iniEncode)
    pbFile.write(YAML.fromPlaybook(pb))
    Process(cmd, cwd = None, env: _*).run(pio)
  }
}
