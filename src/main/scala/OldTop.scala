import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemorySynthInit
import util.{createBuildDir, getRootBankIndex, splitIntoBanks, writeHexSeqToFile}
import chisel3.experimental.{ChiselAnnotation, annotate}

import scala.io.Source


class OldTop(init: Seq[Int], k: Int) extends Module {

  val io = IO(new Bundle {
    val go = Input(Bool())
    val value = Output(UInt(5.W))
  })

  annotate(new ChiselAnnotation {
    override def toFirrtl =
      MemorySynthInit
  })

  def synchronize[T <: Data](x: T): T = RegNext(RegNext(x))
  def risingEdge(x: Bool): Bool = x && !RegNext(x)

  // handle creation of memory initialization files for each of the k memory banks
  createBuildDir()
  splitIntoBanks(k)(init).zipWithIndex.foreach { case (bankInit, index) =>
    writeHexSeqToFile(bankInit, s"build/memory-initialization_$index.txt")
  }

  println(getRootBankIndex(k, init.length))

  // create memory banks
  val banks = Seq.fill(k)(SyncReadMem(init.length / k, UInt(32.W)))
  banks.zipWithIndex.foreach { case (bank, index) =>
    loadMemoryFromFileInline(bank,s"build/memory-initialization_$index.txt")
  }

  val pointer = RegInit(0.U(5.W))

  io.value := 0.U.asTypeOf(io.value)
  io.value := banks(0).read(pointer)
  pointer := Mux(risingEdge(synchronize(io.go)), pointer + 1.U, pointer)

}

object OldTop {
  def main(args: Array[String]): Unit = {
    val testFile = args(args.indexOf("--test-file") + 1)
    val k = args(args.indexOf("--k") + 1).toInt
    val source = Source.fromFile(testFile)
    val testSeq = source.getLines().map(_.toInt).toArray
    emitVerilog(new OldTop(testSeq, k))
  }
}