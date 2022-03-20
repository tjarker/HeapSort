
import HeapTest.HeapWrapper
import chisel3._
import chisel3.util.log2Ceil
import chiseltest._
import lib.{randomParameters, uRand, uRands}
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class HeapTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Heap"

  it should "sort inserted numbers" in {

    Seq.fill(10)(randomParameters()).foreach { p =>
      println(p)
      test(new Heap(p)) { dut =>

        dut.clock.setTimeout(0)

        val testVals = uRands(p.n + 1, p.w.W)

        testVals.foreach { v =>
          dut.insert(v.U)
        }

        val sorted = testVals.sorted.reverse
        sorted.foreach { v =>
          val root = dut.removeRoot().litValue
          assert(v == root)
        }

      }
    }

  }
}

object HeapTest {

  implicit class HeapWrapper(dut: Heap) {
    def insert(x: UInt): Unit = {
      dut.io.newValue.poke(x)
      dut.io.op.poke(Heap.Operation.Insert)
      dut.io.valid.poke(1.B)

      dut.clock.step()

      dut.io.valid.poke(0.B)

      while(!dut.io.ready.peek.litToBoolean) dut.clock.step()
    }
    def removeRoot(): UInt = {
      if(dut.io.empty.peek.litToBoolean) throw new Exception("tried to remove root in empty heap")
      val root = dut.io.root.peek

      dut.io.op.poke(Heap.Operation.RemoveRoot)
      dut.io.valid.poke(1.B)

      dut.clock.step()

      dut.io.valid.poke(0.B)

      while(!dut.io.ready.peek.litToBoolean) dut.clock.step()

      root
    }
  }

}