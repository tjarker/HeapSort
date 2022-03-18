
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class HeapMemoryTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Memory"

  it should "accept reads and writes to all indices" in {
    val configs = Seq(
      Heap.Parameters(256, 2, 8),
      Heap.Parameters(1024, 8, 16),
      Heap.Parameters(4096*8, 4, 64)
    )
    configs.foreach { c =>
      test(new HeapMemory(c)) { dut =>

        // expect all indices to be initialized to zero
        (0 until c.n).foreach { i =>
          dut.io.read.index.poke(i.U)
          dut.clock.step()
          dut.io.read.values(0).expect(0.U)
        }

        // write to all indices
        dut.io.write.valid.poke(1.B)
        (0 until c.n).foreach { i =>
          dut.io.write.index.poke(i.U)
          dut.io.write.value.poke(i.U)
          dut.clock.step()
        }
        dut.io.write.valid.poke(0.B)

        // read from all indices and expect written value
        (0 until c.n).foreach { i =>
          dut.io.read.index.poke(i.U)
          dut.clock.step()
          dut.io.read.values(0).expect(i.U)
        }

      }
    }

  }

}
