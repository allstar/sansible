package ansible

import monocle.macros.GenLens
import monocle.function.{each, at}
import monocle.std.map._
import monocle.std.list._
import monocle._

object Inventory {
  type HostVars = Map[String, String]

  sealed trait Item {
    def hostVars: HostVars
  }

  object Item {
    object Optics {
      val group = Optional[Item, Group] {
        case g: Group => Some(g)
        case _ => None
      }(g => _ =>  g)
    }
  }

  sealed trait HostId extends AnsibleId

  object HostId {
    object Optics {
      val hostnameVars = GenLens[Hostname](_.hostVars)

      def hostVar(name: String): Lens[Hostname, Option[String]] =
        hostnameVars ^|-> at(name)

      val hostname = Optional[HostId, Hostname] {
        case hn: Hostname => Some(hn)
        case _ => None
      }(hn => _ => hn)

      def hostname(name: String): Optional[Hostname, Hostname] =
        Optional[Hostname, Hostname](Some(_).filter(_.id == name))(hn => _ => hn)

    }
  }

  case class HostPattern(id: String) extends HostId

  case class Hostname(id: String,
                      hostVars: HostVars = Map.empty,
                      port: Option[Int] = None) extends Item with HostId


  case class Group(name: String,
                   hosts: List[HostId],
                   hostVars: HostVars = Map.empty,
                   children: List[Group] = Nil) extends Item

  object Group {
    object Optics {
      val hosts = GenLens[Group](_.hosts)
      val vars = GenLens[Group](_.hostVars)

      val hostNames: Traversal[Group, Hostname] =
        hosts ^|->> each ^|-? HostId.Optics.hostname

      def group(name: String): Optional[Group, Group] =
        Optional[Group, Group](Some(_).filter(_.name == name))(g => _ => g)
    }
  }

  object Optics {
    import HostId.{Optics => id}
    import Group.{Optics => g}

    val items = GenLens[Inventory](_.items)

    val groups: Traversal[Inventory, Group] =
      items.composeTraversal(each).composeOptional(Item.Optics.group)

    def groupVar(gName: String, vName: String): Traversal[Inventory, Option[String]] =
      groups ^|-? g.group(gName) ^|-> g.vars ^|-> at(vName)

    def groupVars(gName: String): Traversal[Inventory, HostVars] =
      groups ^|-? g.group(gName) ^|-> g.vars

    def groupHostVar(gName: String, vName: String): Traversal[Inventory, Option[String]] =
      groups ^|-? g.group(gName) ^|->> g.hostNames ^|-> id.hostVar(vName)

    def groupHostVar(gName: String, hName: String, vName: String): Traversal[Inventory, Option[String]] =
      groups ^|-? g.group(gName) ^|->> g.hostNames ^|-? id.hostname(hName) ^|-> id.hostVar(vName)
  }
}

import Inventory._
import Optics._

case class Inventory(items: List[Inventory.Item]) {
  def withGroupVar(gName: String, kV: (String, String)): Inventory =
    groupVar(gName, kV._1).set(Some(kV._2))(this)

  def groupVars(gName: String): Option[HostVars] =
    Optics.groupVars(gName).getAll(this).headOption

  def withHostVar(gName: String, hName: String, kV: (String, String)): Inventory =
    groupHostVar(gName, hName, kV._1).set(Some(kV._2))(this)

  def hostVarValues(gName: String, vName: String): List[String] =
    groupHostVar(gName, vName).getAll(this).flatten
}
