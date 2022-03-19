package util


import chisel3._

object ValidTagged {
  def apply[T <: Data](typeGen: => T): ValidTagged[T] = new ValidTagged(typeGen)
  def apply[T <: Data](valid: Bool, data: T): ValidTagged[T] = {
    val v = Wire(ValidTagged(chiselTypeOf(data)))
    v.valid := valid
    v.data := data
    v
  }
}

class ValidTagged[T <: Data](typeGen: => T) extends Bundle {
  val valid = Bool()
  val data = typeGen
}
