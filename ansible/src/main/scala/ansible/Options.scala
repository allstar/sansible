package ansible

object Options {
  sealed trait Serial {
    def toInt: Int
  }

  case class HostNumber(toInt: Int) extends Serial
  case class HostPercentage(toInt: Int) extends Serial {
    require((0 to 100).contains(toInt), "Should be a percentage")
  }

  sealed trait BecomeMethod extends AnsibleId {
    def id: String
  }

  object Su extends BecomeMethod {
    override def id = "su"
  }
  object Sudo extends BecomeMethod {
    override def id = "sudo"
  }
  object Pbrun extends BecomeMethod {
    override def id = "pbrun"
  }
  object Pfexec extends BecomeMethod {
    override def id = "pfexec"
  }
  object Doas extends BecomeMethod {
    override def id = "doas"
  }
  case class Become(user: Option[String], method: Option[BecomeMethod] = None)
}

import Options._

trait Options {
  def tags: Option[List[String]]
  def env: Option[Map[String, String]]
  def serial: Option[Serial]
  def remoteUser: Option[String]
  def become: Option[Become]
}
