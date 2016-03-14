package ansible

import argonaut._
import Argonaut._

object Module {
  case class MOption(name: String,
                     required: Boolean,
                     default: Option[String],
                     choices: Option[List[String]])

  object MOption {
    implicit val jsonCodec = DecodeJson[MOption](c => {
      def stringOrNull(f: Json): Json =
        if (f.isString) f else jNull

      for {
        name <- (c --\ "option_name").as[String]
        required <- (c --\ "required").as[Boolean] ||| DecodeResult.ok(false)
        default <- (c --\ "default").withFocus(stringOrNull).as[Option[String]]
        choices <- (c --\ "choices").as[Option[List[String]]]
      } yield MOption(name, required, default, choices)
    })
  }

  implicit val jsonCoded = DecodeJson[Module](c => {
    def optionsList(f: Json): Json = {
      val fieldsWithObjs = f.objectFieldsOrEmpty.zip(f.objectValuesOrEmpty)
      val array = jArrayElements(fieldsWithObjs.map { case (fieldName, v) =>
        v.withObject(o => o +("option_name", jString(fieldName)))
      }: _*)
      array
    }
    for {
      pkg  <- (c --\ "package").as[String]
      name <- (c --\ "module").as[String]
      desc <- (c --\ "short_description").as[Option[String]]
      opts <- (c --\ "options").withFocus(optionsList).as[List[MOption]]
    } yield Module(pkg, name, desc, opts)
  })
}
case class Module(pkg: String,
                  name: String,
                  shortDescription: Option[String],
                  options: List[Module.MOption]) {
  def fullName = s"$pkg.$name"
}