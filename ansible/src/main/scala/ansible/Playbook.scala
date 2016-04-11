package ansible

import ansible.CommonOptions._
import ansible.Inventory.HostPattern

object Playbook {
  case class Options(tags: Option[Set[Tag]] = None,
                     env: Option[Map[String, String]] = None,
                     remoteUser: Option[String] = None,
                     anyErrorsFatal: Option[Boolean] = None,
                     serial: Option[Serial] = None,
                     become: Option[Become] = None,
                     maxFailPercentage: Option[Int] = None,
                     connection: Option[String] = None)
}

import Playbook._
case class Playbook(hosts: List[HostPattern],
                    tasks: List[Task],
                    handlers: List[Task] = Nil,
                    options: Playbook.Options = Playbook.Options()) {

  def modifyOptions(f: Options => Options): Playbook =
    copy(options = f(options))
}
