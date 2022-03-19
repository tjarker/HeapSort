
import HeapTest.HeapWrapper
import chisel3._
import chisel3.util.log2Ceil
import chiseltest._
import lib.{uRand, uRands}
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class HeapTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Heap"

  it should "Set new largest as root a new item" in {
    test(new Heap(Heap.Parameters(32,2,8))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.op.poke(Heap.Operation.Insert)
      dut.io.newValue.poke(123.U)
      dut.io.valid.poke(1.B)

      dut.clock.step()
      dut.io.valid.poke(0.B)
      while(!dut.io.ready.peek.litToBoolean) dut.clock.step()

      dut.io.op.poke(Heap.Operation.Insert)
      dut.io.newValue.poke(255.U)
      dut.io.valid.poke(1.B)

      dut.clock.step()
      dut.io.valid.poke(0.B)
      while(!dut.io.ready.peek.litToBoolean) dut.clock.step()

      dut.io.op.poke(Heap.Operation.RemoveRoot)
      dut.io.valid.poke(1.B)

      dut.clock.step()
      dut.io.valid.poke(0.B)
      while(!dut.io.ready.peek.litToBoolean) dut.clock.step()


      dut.io.op.poke(Heap.Operation.RemoveRoot)
      dut.io.valid.poke(1.B)

      dut.clock.step()
      dut.io.valid.poke(0.B)
      while(!dut.io.ready.peek.litToBoolean) dut.clock.step()

    }
  }

  it should "work like scala priority queue" in {

    while (true) {

      val k = scala.math.pow(2,uRand(1 until 6).toInt).toInt
      val params = Seq(
        Heap.Parameters(scala.math.pow(2,uRand(log2Ceil(k) until 16).toInt).toInt, k, scala.math.pow(2,uRand(3 until 6).toInt).toInt),
        //Heap.Parameters(2048, 4, 16),
        //Heap.Parameters(2048, 8, 32),
        //Heap.Parameters(4096, 16, 64),
      )
      params.foreach { p =>
        println(p)
        test(new Heap(p)) { dut =>

          dut.clock.setTimeout(0)

          val testVals = uRands(p.n + 1, p.w.W) //Seq(176, 45, 156, 189, 189, 120, 111, 105, 118)
          //println(testVals.mkString(", "))

          testVals.foreach { v =>
            dut.insert(v.U)
            //print(s"${dut.io.root.peek.litValue}, ")
          }
          println()

          val sorted = testVals.sorted.reverse
          sorted.foreach { v =>
            val root = dut.removeRoot().litValue
            //println(s"$v -> $root")
            assert(v == root)
          }

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