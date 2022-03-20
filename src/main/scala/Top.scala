import Top.State
import util.writeHexSeqToFile
import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemorySynthInit
import chisel3.experimental.{ChiselAnnotation, ChiselEnum, annotate}
import chisel3.util._

import scala.io.Source

object Top {
  object State extends ChiselEnum {
    val Idle, IssueInsert, WaitInsert, IssueRemove, WaitRemove, Done = Value
  }
}

class Top(params: Heap.Parameters, init: Seq[Int]) extends Module {

  val io = IO(new Bundle {
    val done = Output(Bool())
  })

  annotate(new ChiselAnnotation {
    override def toFirrtl = MemorySynthInit
  })

  val memory = SyncReadMem(init.length, UInt(32.W))
  writeHexSeqToFile(init, "build/memory-initialization.txt")
  loadMemoryFromFileInline(memory, "build/memory-initialization.txt")

  val heap = Module(new Heap(params))

  val stateReg = RegInit(State.IssueInsert)
  val pointerReg = RegInit(0.U(log2Ceil(init.length + 1).W))

  val memOut = memory.read(pointerReg)

  io.done := 0.B
  heap.io.valid := 0.B
  heap.io.op := DontCare
  heap.io.newValue := DontCare

  switch(stateReg) {
    is(State.IssueInsert) {
      heap.io.op := Heap.Operation.Insert
      heap.io.newValue := memOut
      heap.io.valid := 1.B
      pointerReg := pointerReg + 1.U
      stateReg := State.WaitInsert
    }
    is(State.WaitInsert) {
      stateReg := Mux(heap.io.ready, Mux(pointerReg === init.length.U, State.IssueRemove, State.IssueInsert), State.WaitInsert)
    }
    is(State.IssueRemove) {
      heap.io.op := Heap.Operation.RemoveRoot
      heap.io.valid := 1.B
      memory.write(pointerReg, heap.io.root)
      pointerReg := pointerReg - 1.U
      stateReg := State.WaitRemove
    }
    is(State.WaitRemove) {
      stateReg := Mux(heap.io.ready, Mux(heap.io.empty, State.Done, State.IssueRemove), State.WaitRemove)
    }
    is(State.Done) {
      io.done := 1.B
      stateReg := State.Done
    }

  }
}

object TopEmitter extends App {
    //val testFile = args(args.indexOf("--test-file") + 1)
    //val source = Source.fromFile(testFile)
    //val testSeq = source.getLines().map(_.toInt).toArray
    emitVerilog(new Top(Heap.Parameters(16*1024,16,32), Seq.fill(16*1024)(BigInt(32, scala.util.Random).toInt)))
}