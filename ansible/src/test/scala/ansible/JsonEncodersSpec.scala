package ansible


import org.scalatest.FreeSpec
import Modules._
import Inventory._
import argonaut._
import Argonaut._
import JsonEncoders._

class JsonEncodersSpec extends FreeSpec {
  "json encoders" - {
    "can serialise a playbook" in {
      val f = File(path="/foo/bar", state=Some(File.State.file))
      val t1 = Task("create foo/bar", f, Task.Options(tags = Some(List("tag1", "tag2"))))
      val t2 = Task("ping host", Ping())
      val pb = Playbook(List(HostPattern("example-host")), t1 :: t2 :: Nil, Playbook.Options(connection = Some("local")))

      val json = Parse.parse("""{
          |  "hosts": ["example-host"],
          |  "connection": "local",
          |  "tasks": [
          |    {"name": "create foo/bar", "file": {"args": {"state": "file", "path": "/foo/bar"}}, "tags": ["tag1", "tag2"]},
          |    {"name": "ping host", "ping": {"args": {}}}
          |  ]
          |}""".stripMargin)

      assertResult(json.toOption.get)(pb.asJson)
    }
  }
}
