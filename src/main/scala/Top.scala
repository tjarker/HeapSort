import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemorySynthInit
import util.writeHexSeqToFile
import chisel3.experimental.{ChiselAnnotation, annotate}

import scala.io.Source


class Top(init: Seq[Int]) extends Module {

  val io = IO(new Bundle {
    val done = Output(Bool())
  })

  annotate(new ChiselAnnotation {
    override def toFirrtl =
      MemorySynthInit
  })

  val memory = SyncReadMem(init.length, UInt(32.W))
  writeHexSeqToFile(init, "build/memory-initialization.txt")
  loadMemoryFromFileInline(memory, "build/memory-initialization.txt")


}

object Top extends App {
    val testFile = args(args.indexOf("--test-file") + 1)
    val source = Source.fromFile(testFile)
    val testSeq = source.getLines().map(_.toInt).toArray
    emitVerilog(new Top(testSeq))
}