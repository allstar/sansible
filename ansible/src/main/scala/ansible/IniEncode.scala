package ansible

trait IniEncode[T] {
  def encode(o: T): String
}
object IniEncode {
  implicit class IniOps[T: IniEncode](o: T) {
    def iniEncode: String = implicitly[IniEncode[T]].encode(o)
  }
}

