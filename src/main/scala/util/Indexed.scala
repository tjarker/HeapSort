package util

import chisel3._
import chisel3.internal.firrtl.Width

object Indexed {
  def fromTuple[T <: Data](init: (T,UInt)): Indexed[T] = {
    val w = Wire(new Indexed(init._2.getWidth.W, chiselTypeOf(init._1)))
    w.item := init._1
    w.index := init._2
    w
  }
  def apply[T <: Data](indexWidth: Width, typeGen: => T): Indexed[T] = new Indexed(indexWidth, typeGen)
}
class Indexed[T <: Data](indexWidth: Width, typeGen: => T) extends Bundle {
  val item = typeGen
  val index = UInt(indexWidth)
}