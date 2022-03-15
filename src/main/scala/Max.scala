
import chisel3._

object Max {

  object Indexed {
    def fromTuple(init: (UInt,Int)): Indexed = {
      val w = Wire(new Indexed)
      w.item := init._1
      w.index := init._2.U
      w
    }
  }
  class Indexed extends Bundle {
    val item = UInt(32.W)
    val index = UInt()
  }

  def apply(values: UInt*): Indexed =
    VecInit(values.zipWithIndex.map(Indexed.fromTuple)).reduceTree { (left,right) =>
      Mux(left.item > right.item, left, right)
    }

}
