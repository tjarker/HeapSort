
import HeapTest.HeapWrapper
import chisel3._
import chiseltest._
import lib.{randomParameters, uRands}
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable.ArrayBuffer

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
          val (_,root) = dut.removeRoot()
          assert(v == root.litValue)
        }

      }
    }
  }

}



object HeapTest {

  implicit class HeapWrapper(dut: Heap) {
    def insert(x: UInt): Int = {
      var steps = 0

      dut.io.newValue.poke(x)
      dut.io.op.poke(Heap.Operation.Insert)
      dut.io.valid.poke(1.B)

      dut.clock.step()
      steps += 1

      dut.io.valid.poke(0.B)

      while(!dut.io.ready.peek.litToBoolean) {
        dut.clock.step()
        steps += 1
      }

      steps
    }
    def removeRoot(): (Int,UInt) = {
      if(dut.io.empty.peek.litToBoolean) throw new Exception("tried to remove root in empty heap")
      var steps = 0

      val root = dut.io.root.peek

      dut.io.op.poke(Heap.Operation.RemoveRoot)
      dut.io.valid.poke(1.B)

      dut.clock.step()
      steps += 1

      dut.io.valid.poke(0.B)

      while(!dut.io.ready.peek.litToBoolean) {
        dut.clock.step()
        steps += 1
      }
      (steps,root)
    }
  }

}