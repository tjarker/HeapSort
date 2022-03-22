

import chisel3._
import chiseltest._
import lib.randomParameters
import org.scalatest.flatspec.AnyFlatSpec

class SwapperTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Swapper"

  it should "create two write requests" in {
    test(new Swapper(Heap.Parameters(32, 4, 32))) { dut =>

      val values = Seq(0xDEADBEEFL, 123)
      val indices = Seq(5,20)

      dut.io.req.valid.poke(1.B)
      dut.io.req.values.map(_.item).zip(values).foreach { case (p,v) => p.poke(v.U) }
      dut.io.req.values.map(_.index).zip(indices).foreach { case (p,v) => p.poke(v.U) }

      dut.io.req.ready.expect(1.B)
      dut.io.mem.valid.expect(0.B)

      dut.clock.step() ///////////////////////////////////////////////////////////////////////////

      dut.io.req.valid.poke(0.B)

      dut.io.mem.index.expect(indices(0).U)
      dut.io.mem.value.expect(values(1).U)
      dut.io.mem.valid.expect(1.B)

      dut.io.req.ready.expect(0.B)

      dut.clock.step() ///////////////////////////////////////////////////////////////////////////

      dut.io.mem.index.expect(indices(1).U)
      dut.io.mem.value.expect(values(0).U)
      dut.io.mem.valid.expect(1.B)

      dut.io.req.ready.expect(0.B)

      dut.clock.step() ///////////////////////////////////////////////////////////////////////////

      dut.io.mem.valid.expect(0.B)

      dut.io.req.ready.expect(1.B)

    }
  }

}
