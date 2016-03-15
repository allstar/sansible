package ansible

import org.scalatest.{Matchers, FreeSpec}
import argonaut._
import Argonaut._

import scalaz.Success

class AnsibleModuleTest extends FreeSpec with Matchers {
  import AnsibleModule._
  "ModuleOption can be decoded" in {
    val input =
      """
        |{
        |  "option1": {
        |    "description": [
        |      "desc"
        |    ],
        |    "required": true,
        |    "default": null
        |  },
        |  "option2": {
        |    "required": false,
        |    "default": "yes",
        |    "choices": [ "yes", "no" ],
        |    "description": [ "desc" ]
        |  },
        |  "option3": {
        |    "description": ["desc"],
        |    "choices": ["a", "b", "c"]
        |  }
        |
        |}
        |""".stripMargin

    // S => T

    val option1 = StringOption(
      "option1",
      "desc",
      required = true
    )

    val option2 = BooleanOption(
      "option2",
      "desc",
      required = false
    )
    val option3 = EnumOption(
      "option3",
      "desc",
      choices = List("a", "b", "c"),
      required = false)

    input.decodeValidation[List[ModuleOption]] should ===(Success(List(
      option1, option2, option3
    )))
  }
}