package ansible

import ansible.Inventory._
import ansible.IniEncode._
import ansible.IniEncoders._

import org.scalatest.FreeSpec

class IniEncodersSpec extends FreeSpec {
  "an inventory" - {
    val host1 = Hostname(id = "host1", port = Some(4444))
    val host2 = Hostname(id = "host2",
      hostVars = HostVars(Map(
        "ansible_user" -> "user",
        "ansible_ssh_pass" -> "secret")))
    val host3 = HostPattern(id = "host*")
    val group1 = Group("group1",
      List(host3), HostVars(Map(
        "group_var1" -> "var1"
      )))
    val group2 = Group(
      name = "group2",
      hosts = Nil,
      children = List(group1))

    "when it contains hostIds only" - {
      "will not contain headings" in {
        assertResult(
          """host1:4444
            |host2 ansible_user=user ansible_ssh_pass=secret""".stripMargin)(
          Inventory(host1 :: host2 :: Nil).iniEncode)
      }
    }

    "when it contains hostids and groups" - {
      "will include the group name and vars" in {
        assertResult(
          """host1:4444
            |
            |[group1]
            |host*
            |
            |[group1:vars]
            |group_var1=var1""".stripMargin)(
          Inventory(List(
            host1,
            Group("group1", List(host3), HostVars(Map(
              "group_var1" -> "var1"
            )))
          )).iniEncode)
      }

      "will include the group children" in {
         assertResult(
          """host1:4444
            |
            |[group1]
            |host*
            |
            |[group1:vars]
            |group_var1=var1
            |
            |[group2:children]
            |group1""".stripMargin)(Inventory(List(host1, group1, group2)).iniEncode)
      }
    }
  }
}
