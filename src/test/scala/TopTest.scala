
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Top"

  it should "work" in {
    val testSeq = Seq.tabulate(32)(i => i)
    test(new Top(testSeq)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      0 until 25 foreach { i =>
        dut.clock.step()
      }
    }
  }

}
