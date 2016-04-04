package ansible

import Inventory._
import IniEncode._

object IniEncoders {
  private def encodeVars(o: HostVars): String =
    o.map { case (k, v) => s"$k=$v" }.mkString(" ")

  implicit val hostname = new IniEncode[Hostname] {
    override def encode(o: Hostname): String = {
      val vars = if (o.hostVars.isEmpty) "" else " " + encodeVars(o.hostVars)
      o.id + o.port.fold("")(p => s":$p") + vars
    }
  }

  implicit val hostPattern = new IniEncode[HostPattern] {
    override def encode(o: HostPattern): String = o.id
  }

  implicit val hostId = new IniEncode[HostId] {
    override def encode(o: HostId): String = o match {
      case hp: HostPattern => hp.iniEncode
      case hn: Hostname => hn.iniEncode
    }
  }

  implicit val group = new IniEncode[Group] {
    private def heading(s: String) = s"\n[$s]"

    override def encode(o: Group): String = {
      val hostLines =
        if (o.hosts.isEmpty) Nil
        else heading(o.name) :: o.hosts.map(_.iniEncode)

      val hostVars =
        if (o.hostVars.isEmpty) Nil
        else heading(s"${o.name}:vars") :: encodeVars(o.hostVars) :: Nil

      val children =
        if (o.children.isEmpty) Nil
        else heading(s"${o.name}:children") :: o.children.map(_.name)

      (hostLines ++ hostVars ++ children).mkString("\n")
    }
  }

  implicit val item = new IniEncode[Item] {
    override def encode(o: Item): String = o match {
      case h: Hostname => h.iniEncode
      case g: Group => g.iniEncode
    }
  }

  implicit val inventory = new IniEncode[Inventory] {
    override def encode(o: Inventory): String =
      o.items.map(_.iniEncode).mkString("\n")
  }
}
