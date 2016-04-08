package ansible

import scala.io.Source
import scala.sys.process.{Process, ProcessIO}

import ansible.IniEncode._
import ansible.IniEncoders._
import better.files.File
import com.typesafe.scalalogging.LazyLogging

object Runner extends LazyLogging {
  def runPlaybook(inv: Inventory)(pb: Playbook, opts: Option[String] = None): Unit = {
    val invFile = File.newTemporaryFile("ansible-inventory")
    val pbFile  = File.newTemporaryFile("ansible-playbook", ".yml")

    val pio = new ProcessIO(
      _ => (),
      out  => Source.fromInputStream(out).getLines.foreach(println),
      err => Source.fromInputStream(err).getLines.foreach(System.err.println)
    )

    val cmd = s"ansible-playbook ${opts.getOrElse("")} -i ${invFile.path} ${pbFile.path}"
    val env = Seq("ANSIBLE_FORCE_COLOR" -> "true")
    val process = Process(cmd, cwd = None, env: _*).run(pio)

    invFile.write(inv.iniEncode)
    pbFile.write(YAML.fromPlaybook(pb))
    logger.info(cmd)

    val exitCode = process.exitValue()
    logger.info(s"run completed with exit code: $exitCode")
    process.destroy()
  }
}
