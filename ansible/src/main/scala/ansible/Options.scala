package ansible

object Options {
  sealed trait Serial {
    def toInt: Int
  }

  case class HostNumber(toInt: Int) extends Serial
  case class HostPercentage(toInt: Int) extends Serial {
    require((0 to 100).contains(toInt), "Should be a percentage")
  }
}
import Options._

trait Options {
  def tags: Option[List[String]]
  def env: Option[Map[String, String]]
  def serial: Option[Serial]
  def remoteUser: Option[String]
}
