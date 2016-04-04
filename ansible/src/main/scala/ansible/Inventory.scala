package ansible

import monocle.macros.GenLens
import monocle.function.{each,at}
import monocle.std.map._
import monocle.std.list._
import monocle.{Traversal, Optional}

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
                   children: List[Group] = Nil) extends Item

  object Optics {
    val items = GenLens[Inventory](_.items)

    val groupHosts = GenLens[Group](_.hosts)

    val hostnameVars = GenLens[Hostname](_.hostVars)

    val groupItem = Optional[Item, Group] {
      case g: Group => Some(g)
      case _ => None
    }(g => _ =>  g)

    val groups: Traversal[Inventory, Group] =
      items.composeTraversal(each).composeOptional(groupItem)

    val groupHostname = Optional[HostId, Hostname]{
      case hn: Hostname => Some(hn)
      case _ => None
    }(hn => _ => hn)

    val groupHostNames: Traversal[Group, Hostname] =
      groupHosts.composeTraversal(each).composeOptional(groupHostname)

    def groupName(name: String): Optional[Group, Group] =
      Optional[Group, Group](g => if (g.name == name) Some(g) else None)(g => _ => g)

    def groupHostVar(gName: String, vName: String): Traversal[Inventory, Option[String]] =
      groups ^|-? groupName(gName) ^|->> groupHostNames ^|-> hostnameVars ^|-> at(vName)
  }
}

import Inventory._
case class Inventory(items: List[Inventory.Item]) {
  def hostVarValues(gName: String, vName: String): List[String] =
    Optics.groupHostVar(gName, vName).getAll(this).flatten
}
