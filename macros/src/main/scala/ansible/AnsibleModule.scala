package ansible

import argonaut.Argonaut._
import argonaut._

object AnsibleModule {
  sealed trait ModuleOption {
    def name: String
    def description: String
    def required: Boolean
  }

  case class BooleanOption(name: String,
                           description: String,
                           required: Boolean) extends ModuleOption

  case class StringOption(name: String,
                          description: String,
                          required: Boolean) extends ModuleOption

  case class EnumOption(name: String,
                        description: String,
                        required: Boolean,
                        choices: List[String]) extends ModuleOption

  implicit def DecodeModuleOption: DecodeJson[List[ModuleOption]] = DecodeJson(c => {
    val optionNames = c.fields.getOrElse(
      throw new Exception("Expected module to be an object")
    )


    val options = optionNames.map { name =>
      val o = c --\ name
      val bool2str = (v: Boolean) => if (v) "yes" else "no"

      for {
        description <- (o --\ "description").as[List[String]].map(_.mkString("\n")) ||| (o --\ "description").as[String]
        required <- (o --\ "required").as[Boolean] ||| DecodeResult.ok(false)
        choices <- (o --\ "choices").as[List[String]] |||
                   (o --\ "choices").as[List[Boolean]].map(_.map(bool2str)) |||
                    DecodeResult.ok(List[String]())

      } yield choices.map(_.toLowerCase).sorted.distinct match {
        case Nil => StringOption(name, description, required)
        case Seq("no", "yes") =>
          BooleanOption(name, description, required)
        case _ =>
          EnumOption(name, description, required, choices)
      }
    }

    options.foldLeft[DecodeResult[List[ModuleOption]]](DecodeResult.ok(List())) { (acc, res) =>
      for {
        a <- acc
        r <- res
      } yield a ++ List(r)
    }
  })

  implicit def DecodeAnsibleModule: DecodeJson[AnsibleModule] = DecodeJson(c =>
    for {
      name <- (c --\ "module").as[String]
      description <- (c --\ "description").as[List[String]]
      options <- (c --\ "options").as[List[ModuleOption]] ||| DecodeResult.ok[List[ModuleOption]](Nil)
    } yield AnsibleModule(name, description, options)
  )
}

import ansible.AnsibleModule._

case class AnsibleModule(name: String,
                         description: List[String],
                         options: List[ModuleOption])
