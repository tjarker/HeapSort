import Types.MemoryIO
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import Heapifier.State

object Heapifier {

  object State extends ChiselEnum {
    val Idle, ReceiveParent, ReceiveLeftChild, ReceiveRightChildAndCompare, RequestLeftChild = Value
  }

}

class Heapifier(heapSize: Int) extends Module {

  val io = IO(new Bundle {
    val memory = Flipped(new MemoryIO)
    val request = Flipped(Decoupled(new Bundle {
      val index = UInt(log2Ceil(heapSize).W)
    }))
  })
  io.memory := 0.U.asTypeOf(io.memory)

  def leftChildIndex(x: UInt): UInt = (x << 1).asUInt
  def rightChildIndex(x: UInt): UInt = (x << 1).asUInt + 1.U

  val stateReg = RegInit(State.Idle)


  val indexReg = Reg(UInt(log2Ceil(heapSize).W))
  val parentReg = Reg(UInt(32.W))
  val leftChildReg = Reg(UInt(32.W))
  val rightChild = WireDefault(io.memory.readValue)
  val captureLargest = WireDefault(0.B)
  val largest = RegEnable(Max(leftChildReg, rightChild, parentReg), captureLargest)
  val largestIsParent = largest.index === 0.U
  val nextParent = indexReg + largest.index

  switch(stateReg) {
    is(State.Idle) {
      io.request.ready := 1.B
      indexReg := io.request.bits.index
      io.memory.readAddress := io.request.bits.index
      when(io.request.valid) {
        stateReg := State.ReceiveParent
      }
    }
    is(State.ReceiveParent) {
      parentReg := io.memory.readValue

      io.memory.readAddress := leftChildIndex(indexReg)

      stateReg := State.ReceiveLeftChild
    }
    is(State.ReceiveLeftChild) {

      leftChildReg := io.memory.readValue

      io.memory.readAddress := rightChildIndex(indexReg)

      stateReg := State.ReceiveRightChildAndCompare
    }
    is(State.ReceiveRightChildAndCompare) {
      captureLargest := 1.B

      stateReg := State.RequestLeftChild
    }
    is(State.RequestLeftChild) {

      io.memory.writeAddress := indexReg
      io.memory.writeValue := largest.item
      io.memory.writeEnable := !largestIsParent

      io.memory.readAddress := leftChildIndex(nextParent)

      indexReg := nextParent
      parentReg := largest.item

      stateReg := Mux(largestIsParent, State.Idle, State.ReceiveLeftChild)

    }
  }


}
