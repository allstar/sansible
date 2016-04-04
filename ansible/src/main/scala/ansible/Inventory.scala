package ansible

import monocle.macros.GenLens
import monocle._
import Monocle._

object Inventory {
  type HostVars = Map[String, String]

  sealed trait Item {
    def hostVars: HostVars
  }

  sealed trait HostId extends AnsibleId

  case class HostPattern(id: String) extends HostId

  case class Hostname(id: String,
                      hostVars: HostVars = Map.empty,
                      port: Option[Int] = None) extends Item with HostId


  case class Group(name: String,
                   hosts: List[HostId],
                   hostVars: HostVars = Map.empty,
                   children: List[Group] = Nil) extends Item {

    val hostnames = hosts.collect { case h: Hostname => h }
    val hostPatterns = hosts.collect { case hp: HostPattern => hp }
  }

  object Lenses {
    val items = GenLens[Inventory](_.items)

    val groups = Optional[Inventory, List[Group]](_.items.foldRight[Option[List[Group]]](None) {
      case (g: Group, gs) => gs.map(g :: _)
      case (_, gs) => gs
    })(gs => (inv) => inv.copy(items = gs))

    def group(n: String): Optional[List[Group], Group] = Optional[List[Group], Group](_.find(_.name == n))(g => gs =>
      if (gs.contains(g)) gs else gs :+ g
    )

    val hostNames = Optional[Group,List[Hostname]](_.hosts.foldRight[Option[List[Hostname]]](None){
      case (h: Hostname, hs) => hs.map(h :: _)
      case (_, hs) => hs
    })(hs => g => g.copy(hosts = g.hosts ++ hs))

    val hostnameVars = GenLens[Hostname](_.hostVars)
    val groupVars = GenLens[Group](_.hostVars)

    groups.composeOptional(group("foo")).composeOptional(hostNames)

  }
}

import Inventory._
case class Inventory(items: List[Inventory.Item]) {
  def group(name: String): Option[Group] = items.collectFirst {
    case g: Group => g
  }
}
