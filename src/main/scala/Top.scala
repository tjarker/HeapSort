import Top.State
import util.{nextPow2, falling, rising}
import util.Delay
import chisel3._
import chisel3.util._
import firrtl.annotations.MemoryArrayInitAnnotation
import chisel3.experimental.{ChiselAnnotation, ChiselEnum, annotate}

import scala.io.Source


class Top(params: Heap.Parameters, init: Seq[BigInt], lowCycles: Int = 500, highCycles: Int = 15) extends Module {
  import params._

  val io = IO(new Bundle {
    val leds = Output(UInt(4.W))
    val rgb = Output(Bool())
  })

  withReset(!reset.asBool) {

    val memory = SyncReadMem(init.length, UInt(w.W))

    annotate(new ChiselAnnotation {
      override def toFirrtl = MemoryArrayInitAnnotation(memory.toTarget, init)
    })

    val heap = Module(new Heap(params))

    val stateReg = RegInit(State.Setup)
    val pointerReg = RegInit(0.U(log2Ceil(init.length + 1).W))

    val memOut = memory.read(pointerReg)

    val write = WireDefault(0.B)
    when(write) {
      memory.write(pointerReg, heap.io.root)
    }

    heap.io.valid := 0.B
    heap.io.op := DontCare
    heap.io.newValue := DontCare

    val runCounter = RegInit(0.U(log2Ceil(lowCycles).W))
    val blinkReg = RegInit(0.B)

    val rgbController = Module(new LedController(50000000))
    io.leds := blinkReg
    rgbController.io.colors.foreach { c =>
      c.r := 0.U
      c.g := 0.U
      c.b := 0.U
    }
    io.rgb := rgbController.io.out

    switch(stateReg) {
      is(State.Setup) {
        stateReg := State.IssueInsert
        pointerReg := pointerReg + 1.U
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
        when(heap.io.ready && pointerReg === init.length.U) {
          pointerReg := (init.length - 1).U
        }
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
        runCounter := runCounter + 1.U

        when((runCounter === lowCycles.U && !blinkReg) || (runCounter === highCycles.U && blinkReg)) {
          runCounter := 0.U
          blinkReg := !blinkReg
        }

        pointerReg := 0.U
        stateReg := State.Setup
      }

    }
  }
}

object Top {

  object State extends ChiselEnum {
    val Setup, IssueInsert, WaitInsert, IssueRemove, WaitRemove, Done = Value
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

object ManualSetup extends App {

  val testFile = "4K-sorted.txt"
  val k = 2
  val w = 32
  val lowCycles = 500
  val highCycles = 15

  val source = Source.fromFile(testFile)
  val testSeq = source.getLines().map(BigInt(_, 16)).toArray

  emitVerilog(new Top(Heap.Parameters(nextPow2(testSeq.length), k, w), testSeq, lowCycles, highCycles), Array("--target-dir","build"))

}