package ansible

import ansible.AnsibleModule.{EnumOption, StringOption, BooleanOption}

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

class CodeGen(c: whitebox.Context) {
   val u = c.universe
   import u._

   private val s = new Sanitiser(c)
   import s.safeName

   def boolVal(o: BooleanOption): ValDef = {
     val name = termName(o.name)
     (if (o.required) q"val $name: Boolean"
      else q"val $name: Option[Boolean] = None").asInstanceOf[ValDef]
   }

   def enumVal(o: EnumOption, moduleName: String): ValDef = {
     val optionTypeName = typeName(camelize(o.name))
     val moduleTermName = termName(camelize(moduleName))
     val name = termName(o.name)

     (if (o.required)
        q"val $name: $moduleTermName.$optionTypeName"
      else
        q"val $name: Option[$moduleTermName.$optionTypeName] = None").asInstanceOf[ValDef]
   }

   def stringVal(o: StringOption): ValDef = {
     val name = termName(o.name)
     (if (o.required) q"val $name: String"
      else q"val $name: Option[String] = None").asInstanceOf[ValDef]
   }

   def enumClassDef(o: EnumOption): ClassDef = {
     q"""
       sealed trait ${typeName(camelize(o.name))} {
         def id: String
       }
     """.asInstanceOf[ClassDef]
   }

  def enumObjectDef(o: EnumOption): ModuleDef = {
    val optionTermName = termName(camelize(o.name))
    val optionTypeName = typeName(camelize(o.name))

    val caseObjects = o.choices.map { n =>
      val name = sanitize(n)
      val objName = termName(camelize(name))
      q"""
       case object $objName extends $optionTypeName {
         override def id = $n
       }
       """
    }

    val valDefs = o.choices.map { n =>
      val name = sanitize(n)
      val objectTerm = termName(camelize(name))
      val valTerm = termName(name.toLowerCase)
      q"""
        val $valTerm: $optionTypeName = $objectTerm
      """
    }

    q"""
      object $optionTermName {
        ..$caseObjects
        ..$valDefs
        implicit val encoder: EncodeJson[$optionTypeName] = EncodeJson(o =>
          jString(o.id)
        )
      }
    """.asInstanceOf[ModuleDef]
  }

  def moduleObjectJsonEncoder(m: AnsibleModule): ValDef = {
    val moduleTypeName = TypeName(camelize(m.name))
    val moduleTermName = TermName(camelize(m.name))

    val assocList = m.options.map { o =>
      val k = Literal(Constant(o.name))
      val v = TermName(safeName(o.name))
      q"""($k, o.$v.asJson)"""
    }
    q"""
      implicit val encoder: EncodeJson[$moduleTypeName] = EncodeJson(o => {
        val l: List[(String, Json)] = List(..$assocList)
        val args: Json = jObjectAssocList(l.filterNot { case (_, v) => v.isNull })

        jSingleObject(${m.name},
          jSingleObject("args", args)
        )
      })
    """.asInstanceOf[ValDef]
  }


  private def camelize(str: String): String =
    str.split('_').map { s =>
      if (s.isEmpty)
        ""
      else
        s.head.toUpper.toString + s.tail.toLowerCase
    }.mkString

  private def termName(name: String): TermName =
    u.TermName(s"${safeName(name)}")

  private def typeName(name: String): TypeName =
    u.TypeName(s"${safeName(name)}")

  private def sanitize(name: String): String = name match {
    case """ {driveletter}:\sources\sxs""" => "driverletter-sources-sxt"
    case """ {IP}\Share\sources\sxs""" => "ip-share-sources-sxt"
    case "*regex*" => "regex"
    case other =>
      val s = other.replaceAll("\\.","-")
      if (other.headOption.exists(_.isDigit)) s"_$s"
      else s
  }
}
