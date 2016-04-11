package ansible

import ansible.CommonOptions.{Tag, Serial, Become}

object Task {
  case class Options(retry: Option[Int] = None,
                     delegateTo: Option[Inventory.HostId] = None,
                     delegateFacts: Option[Boolean] = None,
                     notifyTask: Option[Task] = None,
                     serial: Option[Serial] = None,
                     async: Option[Int] = None,
                     poll: Option[Int] = None,
                     ignoreErrors: Option[Boolean] = None,
                     runOnce: Option[Boolean] = None,
                     remoteUser: Option[String] = None,
                     become: Option[Become] = None,
                     tags: Option[Set[Tag]] = None,
                     connection: Option[String] = None,
                     env: Option[Map[String, String]] = None)

  def apply(name: String, module: Module, options: Options = Options()): Task =
     Task(name, module.call, options)
}

import Task._
case class Task(name: String,
                module: ModuleCall,
                options: Task.Options) {

  def modifyOptions(f: Options => Options) =
    copy(options = f(options))
}
