package ansible

import org.scalatest.FreeSpec
import Inventory._

class InventorySpec extends FreeSpec {
  trait TestInventory {
    def hosts: List[HostId] = Nil
    def inv = Inventory(List(
      Group("g", hosts, Map("k1" -> "v1"))
    ))
  }
  "Inventory" - {
    "groupHosts" - {
      "gets the list of hosts in a group" in new TestInventory {
        override val hosts = List(
          Hostname("h1"),
          Hostname("h2")
        )

        assertResult(hosts)(inv.groupHosts("g"))
      }
    }
    "withGroupVar" - {
      "sets a group vars" in new TestInventory {
        assertResult(Some(Map("k1" -> "v1", "k2" -> "v2")))(
          inv.withGroupVar("g", "k2" -> "v2").groupVars("g")
        )
      }
    }
    "withHostVar" - {
      "sets a var within a group's host" in new TestInventory {
        override val hosts = List(
          Hostname("h1", Map("hk" -> "hv1")),
          Hostname("h2", Map("hk" -> "hv2"))
        )
        assertResult(List("hv1", "hv2-updated"))(
          inv.withHostVar("g", "h2", ("hk", "hv2-updated"))
            .hostVarValues("g", "hk")
        )
      }
    }
  }
}
