package ansible

object Inventory {
  case class HostVars(get: Map[String, String]) {
    def toStringList = get.map { case (k, v) => s"$k=$v" }.toList
  }

  object HostVars {
    val empty = HostVars(Map.empty)
  }

  sealed trait Item {
    def hostVars: HostVars
  }

  sealed trait HostId extends AnsibleId

  case class HostPattern(id: String) extends HostId {
  }

  case class Hostname(id: String,
                      hostVars: HostVars = HostVars.empty,
                      port: Option[Int] = None) extends Item with HostId


  case class Group(name: String,
                   hosts: List[HostId],
                   hostVars: HostVars = HostVars.empty,
                   children: List[Group] = Nil) extends Item
}

case class Inventory(items: List[Inventory.Item])
