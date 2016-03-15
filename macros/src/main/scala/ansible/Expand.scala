package ansible

import ansible.AnsibleModule._
import argonaut.Argonaut._
import argonaut._
import better.files._

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

@compileTimeOnly("enable macro paradise to expand macro annotations")
class expand extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Expander.expand_impl
}

object Expander {
  val tmpDir = System.getProperty("java.io.tmpdir")

  def expand_impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def reSugar(sym: c.universe.Symbol): String =
      Seq(
        "\\$eq" -> "=",
        "\\$hash" -> "#",
        "\\$bang" -> "!",
        "\\$asInstanceOf" -> "asInstanceOf",
        "\\$isInstanceOf" -> "isInstanceOf"
      ).foldLeft(sym.name.toString) { case (s, (a, b)) => s.replaceAll(a, b) }

    val files = (tmpDir / "parsed_modules").listRecursively.filter(_.extension.contains(".json")).toList

    val objType = c.weakTypeOf[Object]
    val keywords = c.universe.asInstanceOf[scala.reflect.internal.SymbolTable].nme.keywords.map(_.decoded)
    val objMethods = objType.decls.map(reSugar)
    val reservedWords = keywords ++ objMethods

    val ansibleModules = files.map { f =>
      f.contentAsString.decodeValidation[AnsibleModule].fold(err =>
        throw new Exception(s"Failed reading the json representation of the Ansible module: ${f.path}, error: $err"),
        identity
      )
    }

    def safeName(option: ModuleOption) =
      if (reservedWords(option.name)) s"_${option.name}" else option.name

    def generateModule(m: AnsibleModule): (ModuleDef, ClassDef) = {
      val moduleTypeName = TypeName(camelize(m.name))
      val moduleTermName = TermName(camelize(m.name))
      val declarations = m.options.foldLeft((List.empty[ClassDef], List.empty[ModuleDef], List.empty[ValDef])) { case ((enumTypes, enumValues, fields), option) =>
        val name = TermName(s"${safeName(option)}")
        option match {
          case o: BooleanOption =>
            val field =
              if (o.required) q"val $name: Boolean"
              else q"val $name: Option[Boolean] = None"

            (enumTypes, enumValues, field.asInstanceOf[ValDef] :: fields)
          case o: StringOption =>
            val field =
              if (o.required) q"val $name: String"
              else q"val $name: Option[String] = None"

            (enumTypes, enumValues, field.asInstanceOf[ValDef] :: fields)

          case o: EnumOption =>
            val camelName = camelize(safeName(option))
            val optionTypeName = TypeName(camelName)
            val optionTermName = TermName(camelName)
            val caseObjects = o.choices.map { n =>
              val termName = TermName(camelize(n))
              q"""
                  case object $termName extends $optionTypeName {
                    override def id = $n
                  }
              """
            }

            val newEnumType = q"""
              sealed trait $optionTypeName {
                def id: String
              }
            """.asInstanceOf[ClassDef]

            val newEnumValues = q"""
                object $optionTermName {
                  ..$caseObjects
                  implicit val encoder: EncodeJson[$optionTypeName] = EncodeJson(o =>
                    jString(o.id)
                  )
                }
            """.asInstanceOf[ModuleDef]

            val field =
              if (o.required)
                q"val $name: $moduleTermName.$optionTypeName"
              else
                q"val $name: Option[$moduleTermName.$optionTypeName] = None"

            (newEnumType :: enumTypes, newEnumValues :: enumValues, field.asInstanceOf[ValDef] :: fields)
        }
      }

      val (enumTypes, enumValues, fields) = declarations

      val assocList = m.options.map { o =>
        val k = Literal(Constant(o.name))
        val v = TermName(safeName(o))

        q"""($k, o.$v.asJson)"""
      }

      val objectDef = q"""
         object $moduleTermName {
           implicit val encoder: EncodeJson[$moduleTypeName] = EncodeJson(o => {
             val l: List[(String, Json)] = List(..$assocList)
             val args: Json = jObjectAssocList(l.filterNot { case (_, v) => v.isNull })
             jSingleObject(${m.name},
               jSingleObject("args", args)
             )
           })
           ..$enumTypes
           ..$enumValues
         }
      """.asInstanceOf[ModuleDef]

      val classDef = q"""
        case class $moduleTypeName(..$fields) extends ansible.Module
       """.asInstanceOf[ClassDef]

      (objectDef, classDef)
    }

    annottees.map(_.tree) match {
      case List(q"trait $_") =>
        val (objectDefs, classDefs) = ansibleModules.map(m => generateModule(m)).unzip
        c.Expr[Any](
          q"""
             import argonaut._
             import Argonaut._

             ..$objectDefs
             ..$classDefs
          """)
      case _ =>
        c.abort(c.enclosingPosition, "@expand should annotate a trait")
    }
  }

  private def camelize(str: String): String =
    str.split('_').map { s =>
      if (s.isEmpty)
        ""
      else
        s.head.toUpper.toString + s.tail.toLowerCase
    }.mkString
}
