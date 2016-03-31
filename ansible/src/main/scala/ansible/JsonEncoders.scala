package ansible

import ansible.Task._
import ansible.Options._
import argonaut.Argonaut._
import argonaut._

object JsonEncoders {
  implicit val serialEncode = EncodeJson((s: Serial) =>
    s match {
      case HostNumber(n) => jNumber(n)
      case HostPercentage(n) => jString(s"$n%")
    })

  implicit val becomeEncode = EncodeJson((b: Become) => {
    ("become_user" :?= b.user) ->?:
    ("become_method" :?= b.method.map(_.id)) ->?:
    ("become" := true) ->:
    jEmptyObject
  })

  implicit val commonOptionsEncode = EncodeJson((o: ansible.Options) =>
    ("serial" :?= o.serial) ->?:
    ("tags" :?= o.tags) ->?:
    ("env" :?= o.env) ->?:
    ("remote_user" :?= o.remoteUser) ->?:
    merge(jEmptyObject, o.become.asJson)
  )

  implicit val playbookOptionsEncode = EncodeJson((o: Playbook.Options) => {
    val common = o.asInstanceOf[ansible.Options].asJson
    ("connection" :?= o.connection) ->?:
      common
  })

  implicit val optionsEncode = EncodeJson((o: Task.Options) => {
     val common = o.asInstanceOf[ansible.Options].asJson
     val task =
       ("retry" :?= o.retry) ->?:
       ("delegate_to" :?= o.delegateTo.map(_.id)) ->?:
       ("delegate_facts" :?= o.delegateFacts) ->?:
       ("notify" :?= o.notifyTask.map(_.name)) ->?:
       ("serial" :?= o.serial) ->?:
       ("async" :?= o.async) ->?:
       ("poll" :?= o.poll) ->?:
       ("run_once" :?= o.runOnce) ->?:
       jEmptyObject

     merge(common, task)
  })

  private def merge(j1: Json, j2: Json): Json = (j1.obj, j2.obj) match {
    case (Some(a), Some(b)) =>
      b.toList.foldLeft(j1) { case (obj, (k, v)) => obj.->:(k -> v) }
    case (None, Some(b)) => j2
    case (Some(a), None) => j1
    case _ => jEmptyObject
  }

  implicit val taskEncode = EncodeJson((t: Task) =>
    ("name" := t.name) ->:
      merge(t.options.asJson, t.module.json)
  )

  implicit def playbookEncode = EncodeJson((p: Playbook) => {
    val handlers =
      if (p.handlers.isEmpty) jEmptyObject
      else ("handlers" := p.handlers) ->: jEmptyObject

    merge(
      ("hosts" := p.hosts.map(_.id)) ->:
      ("tasks" := p.tasks) ->:
        p.options.asJson, handlers)
  })
}
