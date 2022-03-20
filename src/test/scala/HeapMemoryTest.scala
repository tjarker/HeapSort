
import chisel3._
import chiseltest._
import lib.randomParameters
import org.scalatest.flatspec.AnyFlatSpec

class HeapMemoryTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "HeapMemory"

  it should "accept reads and writes to all indices" in {

    val params = Heap.Parameters(1024, 8, 32)
    test(new HeapMemory(params)) { dut =>

      // expect all indices to be initialized to zero
      (0 until params.n).foreach { i =>
        dut.io.read.index.poke(i.U)
        dut.clock.step()
        dut.io.read.values(0).expect(0.U)
      }

      // write to all indices
      dut.io.write.valid.poke(1.B)
      (0 until params.n).foreach { i =>
        dut.io.write.index.poke(i.U)
        dut.io.write.value.poke(i.U)
        dut.clock.step()
      }
      dut.io.write.valid.poke(0.B)

      // read from all indices and expect written value
      (0 until params.n).foreach { i =>
        dut.io.read.index.poke(i.U)
        dut.clock.step()
        dut.io.read.values(0).expect(i.U)
      }

    }


  }

}
