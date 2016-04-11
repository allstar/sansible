package ansible.std

import ansible.CommonOptions._
import ansible.Playbook.{Options => POpts}
import ansible.Task.{Options => TOpts}
import ansible.{CommonOptions, Playbook, Task}
import monocle.macros.GenLens

trait Options {
  implicit object PlaybookCommonOpts extends CommonOptions[POpts] {
    override def tags(o: POpts) = o.tags
    override def become(o: POpts) = o.become
    override def setBecome(o: POpts, b: Become) = o.copy(become = Some(b))
    override def setTags(o: POpts, ts: Set[Tag]) = o.copy(tags = Some(ts))
    override def setRemoteUser(o: POpts, u: String) = o.copy(remoteUser = Some(u))
    override def remoteUser(o: POpts) = o.remoteUser
    override def setEnv(o: POpts, e: Map[String, String]) = o.copy(env = Some(e))
    override def serial(o: POpts) = o.serial
    override def setSerial(o: POpts, s: Serial) = o.copy(serial = Some(s))
    override def env(o: POpts) = o.env
  }

  implicit object PlaybookOptics extends Optics[Playbook, POpts] {
    override def ev = PlaybookCommonOpts
    override def optionsLens = GenLens[Playbook](_.options)
  }

  implicit object TaskCommonOpts extends CommonOptions[TOpts] {
    override def tags(o: TOpts) = o.tags
    override def become(o: TOpts) = o.become
    override def setBecome(o: TOpts, b: Become) = o.copy(become = Some(b))
    override def setTags(o: TOpts, ts: Set[Tag]) = o.copy(tags = Some(ts))
    override def setRemoteUser(o: TOpts, u: String) = o.copy(remoteUser = Some(u))
    override def remoteUser(o: TOpts) = o.remoteUser
    override def setEnv(o: TOpts, e: Map[String, String]) = o.copy(env = Some(e))
    override def serial(o: TOpts) = o.serial
    override def setSerial(o: TOpts, s: Serial) = o.copy(serial = Some(s))
    override def env(o: TOpts) = o.env
  }

  implicit object TaskOptics extends Optics[Task, TOpts] {
    override def ev = TaskCommonOpts
    override def optionsLens = GenLens[Task](_.options)
  }
}
