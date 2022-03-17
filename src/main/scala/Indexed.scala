import chisel3._

object Indexed {
  def fromTuple[T <: Data](init: (T,UInt)): Indexed[T] = {
    val w = Wire(new Indexed(chiselTypeOf(init._1)))
    w.item := init._1
    w.index := init._2
    w
  }
  def apply[T <: Data](typeGen: => T): Indexed[T] = new Indexed(typeGen)
}
class Indexed[T <: Data](typeGen: => T) extends Bundle {
  val item = typeGen
  val index = UInt()
}