
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class HeapTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Heap"

  it should "insert a new item" in {
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

}
