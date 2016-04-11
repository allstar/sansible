package ansible

import monocle.{Optional, Lens}

object CommonOptions {
  trait AnsibleId {
    def id: String
  }
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
  case class Become(user: Option[String] = None, method: Option[BecomeMethod] = None)

  case class Tag(name: String)


  trait Optics[T, O] {
    private [ansible] def ev: CommonOptions[O]
    private [ansible] def optionsLens: Lens[T, O]

    lazy val serial: Optional[T, Serial] =
      optionsLens ^|-? Optional(ev.serial)(s => o => ev.setSerial(o, s))

    lazy val become: Optional[T, Become] =
      optionsLens ^|-? Optional(ev.become)(b => o => ev.setBecome(o, b))

    lazy val remoteUser: Optional[T, String] =
      optionsLens ^|-? Optional(ev.remoteUser)(u => o => ev.setRemoteUser(o, u))

    lazy val tags: Optional[T, Set[Tag]] =
      optionsLens ^|-? Optional(ev.tags)(ts => o => ev.setTags(o, ts))

    lazy val env: Optional[T, Map[String, String]] =
      optionsLens ^|-? Optional(ev.env)(e => o => ev.setEnv(o, e))
  }
}

import CommonOptions._

trait CommonOptions[T] {
  def tags(o: T): Option[Set[Tag]]
  def env(o: T): Option[Map[String, String]]
  def serial(o: T): Option[Serial]
  def remoteUser(o: T): Option[String]
  def become(o: T): Option[Become]

  def setTags(o: T, ts: Set[Tag]): T
  def setEnv(o: T, e: Map[String, String]): T
  def setSerial(o: T, s: Serial): T
  def setRemoteUser(o: T, u: String): T
  def setBecome(o: T, b: Become): T
}
