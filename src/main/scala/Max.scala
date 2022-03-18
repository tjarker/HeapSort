
import chisel3._

object Max {
  def apply(item: ValidTagged[Indexed[UInt]], items: Vec[ValidTagged[Indexed[UInt]]]): Indexed[UInt] = {
    VecInit(item +: items).reduceTree { (left,right) =>
      Mux(left.valid && left.data.item > right.data.item, left, right)
    }.data
  }
}
