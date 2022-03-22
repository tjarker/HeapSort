
import chisel3._
import chisel3.util.log2Ceil
import chiseltest._
import lib.{randomParameters, uRand}
import org.scalatest.flatspec.AnyFlatSpec

class TopTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Top"

  it should "move the smallest element to address 0" in {

    Seq.fill(10)(randomParameters()).foreach { p =>
      println(p)
      val testSeq = Seq.tabulate(p.n)(i => BigInt(p.w, scala.util.Random))

      println(testSeq(0), testSeq.min)

      test(new Top(p, testSeq)) { dut =>
        dut.clock.setTimeout(0)

        dut.io.go.poke(1.B)

        while (!dut.io.done.peek.litToBoolean) {
          dut.clock.step()
        }

        dut.clock.step(2)
        dut.io.minimum.expect((testSeq.min & 0x7FFF).U)

      }
    }
  }


}
