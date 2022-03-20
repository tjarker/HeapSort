
import chisel3._
import chiseltest._
import lib.randomParameters
import org.scalatest.flatspec.AnyFlatSpec

class MaxFinderTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "MaxFinder"

  it should "find max" in {
    test(new MaxFinder(randomParameters())).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.clock.step(2)

      dut.io.fetcher.parent.item.poke(1.U)
      dut.io.fetcher.children.map(_.data.item).zipWithIndex.foreach { case (p,v) => p.poke((v+2).U) }
      dut.io.fetcher.children.map(_.valid).foreach(_.poke(1.B))

      dut.io.fetcher.valid.poke(1.B)

      dut.clock.step()

      while(!dut.io.res.valid.peek.litToBoolean) dut.clock.step()

    }
  }

}
