package ansible.std

import ansible.CommonOptions._
import ansible.{CommonOptions => CommonOpt, Playbook, Task}
import argonaut.Argonaut._
import argonaut._

trait JsonEncoders {
  implicit val tagEncode = EncodeJson((t: Tag) => jString(t.name))
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

  implicit def commonOptionsEncode[T: CommonOpt]: EncodeJson[T] = EncodeJson((o: T) => {
    val ev = implicitly[CommonOpt[T]]
    ("serial" :?= ev.serial(o)) ->?:
    ("tags" :?= ev.tags(o)) ->?:
    ("env" :?= ev.env(o)) ->?:
    ("remote_user" :?= ev.remoteUser(o)) ->?:
    merge(jEmptyObject, ev.become(o).asJson)
  })


  implicit def playbookOptionsEncode: EncodeJson[Playbook.Options] = EncodeJson((o: Playbook.Options) => {
    ("connection" :?= o.connection) ->?:
      commonOptionsEncode[Playbook.Options].encode(o)
  })

  implicit val optionsEncode = EncodeJson((o: Task.Options) => {
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

     merge(commonOptionsEncode[Task.Options].encode(o), task)
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
