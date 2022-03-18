
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FetcherTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Fetcher"

  it should "create the right read requests" in {
    test(new Fetcher(Heap.Parameters(32,4,8))) { dut =>

      val parentIndex = 2
      val parent = 123
      val childrenIndex = Seq.tabulate(4)(i => 4*parentIndex + i + 1)
      val children = Seq(1,99,23,255)

      dut.clock.step(2) ///////////////////////////////////////////////////////////////////////////

      dut.io.req.valid.poke(1.B)
      dut.io.req.index.poke(2.U)

      dut.io.res.valid.expect(0.B)

      dut.clock.step() ///////////////////////////////////////////////////////////////////////////

      dut.io.req.valid.poke(0.B)

      dut.io.mem.index.expect(parentIndex.U)
      dut.io.mem.withSiblings.expect(0.B)

      dut.io.res.valid.expect(0.B)

      dut.clock.step() ///////////////////////////////////////////////////////////////////////////

      dut.io.mem.values(0).poke(parent.U)

      dut.io.mem.index.expect(childrenIndex.head.U)
      dut.io.mem.withSiblings.expect(1.B)

      dut.io.res.valid.expect(0.B)

      dut.clock.step() ///////////////////////////////////////////////////////////////////////////

      dut.io.mem.values.zip(children).foreach { case (p,c) => p.poke(c.U) }

      dut.io.res.valid.expect(0.B)

      dut.clock.step() ///////////////////////////////////////////////////////////////////////////

      dut.io.res.parent.item.expect(parent.U)
      dut.io.res.parent.index.expect(parentIndex.U)
      dut.io.res.children.zip(children).foreach { case (p,c) => p.data.item.expect(c.U) }
      dut.io.res.children.zip(childrenIndex).foreach { case (p,c) => p.data.index.expect(c.U) }
      dut.io.res.valid.expect(1.B)

    }
  }

}
