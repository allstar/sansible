package ansible

import ansible.AnsibleModule._
import argonaut.Argonaut._
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
    val gen = new CodeGen(c)

    val files = (tmpDir / "parsed_modules").listRecursively.filter(_.extension.contains(".json")).toList
    val ansibleModules = files.map { f =>
      f.contentAsString.decodeValidation[AnsibleModule].fold(err =>
        throw new Exception(s"Failed reading the json representation of the Ansible module: ${f.path}, error: $err"),
        identity
      )
    }

    def generateModule(m: AnsibleModule): (ModuleDef, ClassDef) = {
      val moduleTypeName = TypeName(camelize(m.name))
      val moduleTermName = TermName(camelize(m.name))
      val sortedOptions  = m.options.sortBy(_.required)(Ordering[Boolean].reverse)

      val (enumTypes, enumValues, fields) =
        sortedOptions.foldLeft((List.empty[ClassDef], List.empty[ModuleDef], Vector.empty[ValDef])) { case ((enumTypes, enumValues, fields), option) =>
          option match {
            case o: BooleanOption =>
              (enumTypes, enumValues, fields :+ gen.boolVal(o).asInstanceOf[ValDef])
            case o: StringOption =>
              (enumTypes, enumValues, fields :+ gen.stringVal(o).asInstanceOf[ValDef])
            case o: EnumOption =>
              (gen.enumClassDef(o).asInstanceOf[ClassDef] :: enumTypes,
                gen.enumObjectDef(o).asInstanceOf[ModuleDef] :: enumValues,
                fields :+ gen.enumVal(o, m.name).asInstanceOf[ValDef])
          }
        }

      val jsonEncoderVal = gen.moduleObjectJsonEncoder(m).asInstanceOf[ValDef]

      val objectDef = q"""
         object $moduleTermName {
           $jsonEncoderVal
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
