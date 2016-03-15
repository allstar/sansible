package ansible
import argonaut._

object YAML {
  def fromJSON(json: Json): String = {
    "---\n" + go(json, 0)
  }

  private def indent(indentLevel: Int) =
    " " * indentLevel * 2

  private def go(json: Json, indentLevel: Int): String = {
    val ind = indent(indentLevel)
    (json.string.map(s => safeString(s, indentLevel)) orElse
      json.number.flatMap(_.toBigInt).map(n => ind + n.toString) orElse
      json.number.map(n => ind + n.toDouble.toString) orElse
      (if (json.isNull) Some(ind + "null") else None) orElse
      json.bool.map(b => ind + b.toString) orElse
      json.array.map { ary => fromJsonArray(ary, indentLevel) } orElse
      json.obj.map { o => fromJsonObject(o.toList, indentLevel) }
      ).get
  }

  private def safeString(s: String, n: Int): String =
    indent(n) + "|-\n" + s.split("\n").map(line => indent(n + 1) + line).mkString("\n")

  private def fromJsonArray(ary: List[Json], indentLevel: Int): String = ary match {
    case Nil => indent(indentLevel) + "[]"
    case _ =>  ary.map(el => indent(indentLevel) + "-\n" + go(el, indentLevel + 1)).mkString("\n")
  }

  private def fromJsonObject(obj: List[(String, Json)], indentLevel: Int): String = obj match {
    case Nil => indent(indentLevel) + "{}"
    case _ =>  obj.map { case (k, v) => indent(indentLevel) + s"$k:\n" + go(v, indentLevel + 1) }.mkString("\n")
  }
}
