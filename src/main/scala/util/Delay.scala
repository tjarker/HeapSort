package util

import chisel3._

object Delay {
  def apply[T <: Data](x: T, cycles: Int = 1): T = if(cycles == 0) x else RegNext(Delay(x, cycles - 1), init = 0.U.asTypeOf(x))
}
