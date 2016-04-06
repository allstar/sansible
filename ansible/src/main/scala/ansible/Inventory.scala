package ansible

import monocle.macros.GenLens
import monocle.function.{each, at}
import monocle.std.map._
import monocle.std.list._
import monocle.{Lens, Traversal, Optional}

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

    def hostVar(name: String): Lens[Hostname, Option[String]] =
      hostnameVars ^|-> at(name)

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

    def hostname(name: String): Optional[Hostname, Hostname] =
      Optional[Hostname, Hostname](Some(_).filter(_.id == name))(hn => _ => hn)

    val groupHostNames: Traversal[Group, Hostname] =
      groupHosts ^|->> each ^|-? groupHostname

    def groupName(name: String): Optional[Group, Group] =
      Optional[Group, Group](Some(_).filter(_.name == name))(g => _ => g)

    def groupHostVar(gName: String, vName: String): Traversal[Inventory, Option[String]] =
      groups ^|-? groupName(gName) ^|->> groupHostNames ^|-> hostVar(vName)

    def groupHostVar(gName: String, hName: String, vName: String) =
      groups ^|-? groupName(gName) ^|->> groupHostNames ^|-? hostname(hName) ^|-> hostVar(vName)
  }
}

import Inventory._
import Optics._

case class Inventory(items: List[Inventory.Item]) {
  def withHostVar(gName: String, hName: String, kV: (String, String)): Inventory =
    groupHostVar(gName, hName, kV._1).set(Some(kV._2))(this)

  def hostVarValues(gName: String, vName: String): List[String] =
    groupHostVar(gName, vName).getAll(this).flatten
}
