package ansible

import Inventory.HostPattern
import Options.Serial

object Playbook {
  case class Options(tags: Option[List[String]] = None,
                     env: Option[Map[String, String]] = None,
                     remoteUser: Option[String] = None,
                     anyErrorsFatal: Option[Boolean] = None,
                     serial: Option[Serial] = None,
                     maxFailPercentage: Option[Int] = None,
                     connection: Option[String] = None) extends ansible.Options
}

case class Playbook(hosts: List[HostPattern],
                    tasks: List[Task],
                    handlers: List[Task] = Nil,
                    options: Playbook.Options = Playbook.Options())
