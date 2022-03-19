package util

import chisel3.{Mux, UInt, Vec, VecInit, WireDefault}

object Max {
  def apply(item: ValidTagged[Indexed[UInt]], items: Vec[ValidTagged[Indexed[UInt]]]): Indexed[UInt] = {
    VecInit(item +: items).reduceTree { (left, right) =>
      val comp = WireDefault(Mux(right.valid && right.data.item > left.data.item, right, left))
      comp
    }.data
  }
}
