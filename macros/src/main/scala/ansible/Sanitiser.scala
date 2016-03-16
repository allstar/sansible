package ansible

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

class Sanitiser(c: whitebox.Context) {
  private def reSugar(sym: c.universe.Symbol): String =
    Seq(
      "\\$eq" -> "=",
      "\\$hash" -> "#",
      "\\$bang" -> "!",
      "\\$asInstanceOf" -> "asInstanceOf",
      "\\$isInstanceOf" -> "isInstanceOf"
    ).foldLeft(sym.name.toString) { case (s, (a, b)) => s.replaceAll(a, b) }

  private val objType = c.weakTypeOf[Object]
  private val prodType = c.weakTypeOf[Product]
  private val keywords = c.universe.asInstanceOf[scala.reflect.internal.SymbolTable].nme.keywords.map(_.decoded)
  private val reservedWords = keywords ++ objType.decls.map(reSugar) ++ prodType.decls.map(reSugar)

  def safeName(name: String) =
    if (reservedWords(name)) s"_$name" else name
}
