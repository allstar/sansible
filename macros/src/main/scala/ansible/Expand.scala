package ansible

import java.nio.file._

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.io.Source
import scala.language.experimental.macros
import scala.reflect.internal.Names
import scala.reflect.macros.whitebox
import argonaut._
import better.files._
import Argonaut._
import AnsibleModule._

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

    def generateModule(m: AnsibleModule): c.Tree = {
      val className = TypeName(camelize(m.name))
      val fields = m.options.map { option =>
        val safeName = if (reservedWords(option.name)) s"_${option.name}" else option.name
        val name = TermName(s"$safeName")
        q"val $name: String"
      }

      q"""
         case class $className(..$fields)
       """
    }

    annottees.map(_.tree) match {
      case List(q"trait $_") =>
        val caseClasses = ansibleModules.map(m => generateModule(m))
        c.Expr[Any](q"..$caseClasses")
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
