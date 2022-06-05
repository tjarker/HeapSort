import Top.State
import util.{nextPow2, falling, rising}
import util.Delay
import chisel3._
import chisel3.util._
import firrtl.annotations.MemoryArrayInitAnnotation
import chisel3.experimental.{ChiselAnnotation, ChiselEnum, annotate}

import scala.io.Source


class Top(params: Heap.Parameters, init: Seq[BigInt]) extends Module {
  import params._

  val io = IO(new Bundle {
    val done = Output(Bool())
    val go = Input(Bool())
    val minimum = Output(UInt(15.W))
  })

  val memory = SyncReadMem(init.length, UInt(w.W))

  annotate(new ChiselAnnotation {
    override def toFirrtl = MemoryArrayInitAnnotation(memory.toTarget, init)
  })

  val heap = Module(new Heap(params))

  val stateReg = RegInit(State.Idle)
  val pointerReg = RegInit(0.U(log2Ceil(init.length + 1).W))

  val syncedGo = Delay(io.go, 2)

  val memOut = memory.read(pointerReg)
  io.minimum := memOut
  val write = WireDefault(0.B)
  when(write) {
    memory.write(pointerReg, heap.io.root)
  }

  io.done := 0.B
  heap.io.valid := 0.B
  heap.io.op := DontCare
  heap.io.newValue := DontCare

  switch(stateReg) {
    is(State.Idle) {
      stateReg := Mux(syncedGo, State.IssueInsert, State.Idle)
      pointerReg := Mux(syncedGo, pointerReg + 1.U, 0.U)
    }
    is(State.IssueInsert) {
      heap.io.op := Heap.Operation.Insert
      heap.io.newValue := memOut
      heap.io.valid := 1.B
      pointerReg := pointerReg + 1.U
      stateReg := State.WaitInsert
    }
    is(State.WaitInsert) {
      stateReg := Mux(heap.io.ready, Mux(pointerReg === init.length.U, State.IssueRemove, State.IssueInsert), State.WaitInsert)
      when(heap.io.ready && pointerReg === init.length.U) { pointerReg := (init.length - 1).U}
    }
    is(State.IssueRemove) {
      heap.io.op := Heap.Operation.RemoveRoot
      heap.io.valid := 1.B
      write := 1.B
      pointerReg := pointerReg - 1.U
      stateReg := Mux(pointerReg === 0.U, State.Done, State.WaitRemove)
    }
    is(State.WaitRemove) {
      stateReg := Mux(heap.io.ready, State.IssueRemove, State.WaitRemove)
    }
    is(State.Done) {
      io.done := 1.B
      pointerReg := 0.U
      stateReg := State.Done
    }

  }
}

object Top {

  object State extends ChiselEnum {
    val Idle, IssueInsert, WaitInsert, IssueRemove, WaitRemove, Done = Value
  }

  def main(args: Array[String]) = {
    val k = if(args.contains("-k")) args(args.indexOf("-k") + 1).toInt else 4
    val w = if(args.contains("-w")) args(args.indexOf("-w") + 1).toInt else 32
    val targetDir = if(args.contains("--target-dir")) args(args.indexOf("--target-dir") + 1) else "build"
    val testSeq = if(args.contains("--test-file")) {
      val testFile = args(args.indexOf("--test-file") + 1)
      val source = Source.fromFile(testFile)
      source.getLines().map(BigInt(_, 16)).toArray
    } else {
      Array.fill(4096)(BigInt(w,scala.util.Random))
    }
    println("Initial: %8x - %32s".format(testSeq.head, testSeq.head.toString(2)))
    println("Min:     %8x - %32s".format(testSeq.min, testSeq.min.toString(2)))
    emitVerilog(new Top(Heap.Parameters(nextPow2(testSeq.length), k, w), testSeq), Array("--target-dir",targetDir))
  }

}