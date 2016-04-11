package ansible.dsl

import ansible.CommonOptions._
import org.scalatest.FreeSpec

import ansible.{Task, Playbook}
import ansible.Modules.Ping
import ansible.std._

class CommonOptionsSpec extends FreeSpec {
  trait TestData {
    val task = Task("test task", Ping())
    val playbook = Playbook(Nil, task :: Nil, Nil)
  }

  "CommonOptions syntax" - {
    "withTags" in new TestData {
      val expected = Some(Set(Tag("a"), Tag("b")))
      assertResult(expected)(task.withTags("a", "b").tags)
      assertResult(expected)(playbook.withTags("a", "b").tags)
    }
    "addTags" in new TestData {
      assertResult(Some(Set(Tag("a"), Tag("b"), Tag("c"))))(
        task.addingTags("a", "b").addingTags("c").tags
      )
    }
    "withEnv" in new TestData {
      assertResult(Some(Map("A" -> "B")))(
        task.withEnv(Map("A" -> "B")).env
      )
    }
    "mergeEnv" in new TestData {
      assertResult(Some(Map("A" -> "B", "C" -> "D")))(
        task.withEnv(Map("A" -> "B")).mergeEnv(Map("C" -> "D")).env
      )
    }
    "becoming" in new TestData {
      assertResult(Some(Become(None, Some(Sudo))))(task.usingSudo.become)
      assertResult(Some(Become(Some("userX"), Some(Su))))(task.becoming("userX", Su).become)
    }
    "withOptions" in new TestData {
      assertResult(Some("local"))(
        playbook.modifyOptions(_.copy(connection = Some("local")))
          .options.connection
      )
    }
  }
}
