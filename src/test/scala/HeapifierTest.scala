
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import lib.{uRand, uRands}

class HeapifierTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Heapifier"

  it should "initiate swap if necessary and report result" in {
    val params = Heap.Parameters(32, 8, 8)
    import params._

    (0 until 30).foreach { _ =>

      test(new Heapifier(params)) { dut =>

        val parent = uRand(w.W)
        val parentIndex = uRand(0 until (n/k))
        val largest = uRand(w.W)
        val largestIndex = parentIndex + uRand(0 until k)

        dut.io.maxFinder.parent.item.poke(parent.U)
        dut.io.maxFinder.parent.index.poke(parentIndex.U)
        dut.io.maxFinder.largest.item.poke(largest.U)
        dut.io.maxFinder.largest.index.poke(largestIndex.U)
        dut.io.maxFinder.isParent.poke((parent >= largest).B)
        dut.io.maxFinder.valid.poke(1.B)

        dut.io.res.valid.expect(0.B)

        dut.clock.step()

        if (parent >= largest) {

          dut.io.res.valid.expect(1.B)
          dut.io.res.swapped.expect(0.B)

        } else {

          dut.io.swapper.values(0).item.expect(parent.U)
          dut.io.swapper.values(0).index.expect(parentIndex.U)
          dut.io.swapper.values(1).item.expect(largest.U)
          dut.io.swapper.values(1).index.expect(largestIndex.U)
          dut.io.swapper.valid.expect(1.B)

          dut.io.res.valid.expect(0.B)

          dut.clock.step()

          dut.io.swapper.ready.poke(1.B)

          dut.io.res.valid.expect(1.B)
          dut.io.res.swapped.expect(1.B)
          dut.io.res.largest.expect(largestIndex.U)

        }



      }
    }



  }
}

