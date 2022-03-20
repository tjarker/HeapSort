
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Top"

  it should "work" in {
    val testSeq = Seq.tabulate(8)(i => i)
    test(new Top(Heap.Parameters(8,4,32), testSeq)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(5000)
      while(!dut.io.done.peek.litToBoolean) dut.clock.step()
    }
  }

}
