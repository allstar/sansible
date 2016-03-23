package ansible

import Options.Serial

object Task {
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

  case class Become(user: String, method: Option[BecomeMethod])

  case class Options(retry: Option[Int] = None,
                     delegateTo: Option[Inventory.HostId] = None,
                     delegateFacts: Option[Boolean] = None,
                     notifyTask: Option[Task] = None,
                     serial: Option[Serial] = None,
                     async: Option[Int] = None,
                     pool: Option[Int] = None,
                     ignoreErrors: Option[Boolean] = None,
                     runOnce: Option[Boolean] = None,
                     remoteUser: Option[String] = None,
                     become: Option[Become] = None,
                     tags: Option[List[String]] = None,
                     connection: Option[String] = None,
                     env: Option[Map[String, String]] = None) extends ansible.Options

  def apply(name: String, module: Module, options: Options = Options()): Task =
     Task(name, module.call, options)
}

case class Task(name: String,
                module: ModuleCall,
                options: Task.Options)
