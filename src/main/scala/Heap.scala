
import Heap.State
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

object Heap {
  case class Parameters(
                             n: Int, // Maximum number of elements
                             k: Int, // Order of the heap
                             w: Int, // item width
                           ) {
    require(isPow2(n), "The maximum heap size needs to be a power of 2")
    require(isPow2(k), "The order of the heap needs to be a power of 2")
  }
  object Operation extends ChiselEnum {
    val Insert, RemoveRoot = Value
  }
  class Request(params: Heap.Parameters) extends Bundle {
    import params._
    val op = Input(Heap.Operation())
    val newValue = Input(UInt(w.W))
    val root = Output(UInt(w.W))
    val empty = Output(Bool())
    val valid = Input(Bool())
    val ready = Output(Bool())
  }
  object State extends ChiselEnum {
    val Idle, PreFetchTail,
    WriteTailToRoot, IssueFetchDown, WaitForFetcherDown, IssueSwapDown, WaitForSwapperDown,
    WriteNewTail, IssueFetchUp, WaitForFetcherUp, IssueSwapUp, WaitForSwapperUp
    = Value
  }
}
// TODO: cache tail in idle
class Heap(params: Heap.Parameters) extends Module {
  import params._

  val io = IO(new Heap.Request(params))

  object Components {
    val fetcher = Module(new Fetcher(params))
    val swapper = Module(new Swapper(params))
    val memory = Module(new HeapMemory(params))
  }

  Components.memory.io.read <> Components.fetcher.io.mem
  Components.memory.io.write <> Components.swapper.io.mem
  Components.fetcher.io.req := DontCare
  Components.fetcher.io.req.valid := 0.B
  Components.swapper.io.req := DontCare
  Components.swapper.io.req.valid := 0.B

  val stateReg = RegInit(State.Idle)
  val sizeReg = RegInit(0.U(log2Ceil(n+1).W))
  val tailIndex = sizeReg - 1.U
  val tailReg = RegInit(0.U(w.W))
  val newValueReg = RegInit(0.U(w.W))
  val subTreeIndexReg = RegInit(0.U(log2Ceil(n).W))
  val maxItem = RegEnable(
    Max(ValidTagged(1.B, Components.fetcher.io.res.parent), Components.fetcher.io.res.children),
    Components.fetcher.io.res.valid
  )
  val swapRequired = maxItem.index =/= subTreeIndexReg

  def parent(index: UInt): UInt = ((index - 1.U) >> log2Ceil(k)).asUInt
  def firstChild(index: UInt): UInt = (index >> log2Ceil(k)).asUInt + 1.U

  io.ready := 0.B
  io.root := Components.memory.io.root
  io.empty := sizeReg === 0.U

  switch(stateReg) {
    is(State.Idle) {
      io.ready := 1.B

      Components.memory.io.read.index := tailIndex
      tailReg := Components.memory.io.read.values(0)

      newValueReg := io.newValue

      when(io.valid) {
        when(io.op === Heap.Operation.Insert) {
          stateReg := State.WriteNewTail
        } otherwise {
          when(sizeReg === 1.U) {
            sizeReg := 0.U
            stateReg := State.Idle
          } otherwise {
            stateReg := State.WriteTailToRoot
          }
        }
      } otherwise {
        stateReg := State.Idle
      }

    }
    is(State.WriteNewTail) {
      Components.memory.io.write.index := sizeReg
      Components.memory.io.write.value := newValueReg
      Components.memory.io.write.valid := 1.B
      sizeReg := sizeReg + 1.U
      subTreeIndexReg := parent(sizeReg)

      stateReg := Mux(sizeReg === 0.U, State.PreFetchTail, State.IssueFetchUp)
    }
    is(State.IssueFetchUp) {
      Components.fetcher.io.req.size := sizeReg
      Components.fetcher.io.req.index := subTreeIndexReg
      Components.fetcher.io.req.valid := 1.B

      stateReg := State.WaitForFetcherUp
    }
    is(State.WaitForFetcherUp) {
      stateReg := Mux(Components.fetcher.io.res.valid, State.IssueSwapUp, State.WaitForFetcherUp)
    }
    is(State.IssueSwapUp) {
      Components.swapper.io.req.values(0) := Components.fetcher.io.res.parent
      Components.swapper.io.req.values(1) := maxItem
      when(swapRequired) {
        Components.swapper.io.req.valid := 1.B
        stateReg := State.WaitForSwapperUp
      } otherwise {
        stateReg := State.PreFetchTail
      }
    }
    is(State.WaitForSwapperUp) {
      when(Components.swapper.io.req.ready) { subTreeIndexReg := parent(subTreeIndexReg) }

      stateReg := Mux(Components.swapper.io.req.ready, Mux(subTreeIndexReg === 0.U, State.Idle, State.IssueFetchUp), State.WaitForSwapperUp)

      Components.memory.io.read.index := tailIndex
    }
    is(State.PreFetchTail) {
      Components.memory.io.read.index := tailIndex
      stateReg := State.Idle
    }


    is(State.WriteTailToRoot) {
      Components.memory.io.write.index := 0.U
      Components.memory.io.write.value := tailReg
      Components.memory.io.write.valid := 1.B

      sizeReg := sizeReg - 1.U
      subTreeIndexReg := 0.U

      stateReg := Mux(sizeReg === 2.U, State.PreFetchTail, State.IssueFetchDown)
    }
    is(State.IssueFetchDown) {
      Components.fetcher.io.req.size := sizeReg
      Components.fetcher.io.req.index := subTreeIndexReg
      Components.fetcher.io.req.valid := 1.B

      stateReg := State.WaitForFetcherDown
    }
    is(State.WaitForFetcherDown) {
      stateReg := Mux(Components.fetcher.io.res.valid, State.IssueSwapDown, State.WaitForFetcherDown)
    }
    is(State.IssueSwapDown) {
      Components.swapper.io.req.values(0) := Components.fetcher.io.res.parent
      Components.swapper.io.req.values(1) := maxItem
      when(swapRequired) {
        Components.swapper.io.req.valid := 1.B
        stateReg := State.WaitForSwapperUp
      } otherwise {
        stateReg := State.PreFetchTail
      }
    }
    is(State.WaitForSwapperDown) {
      when(Components.swapper.io.req.ready) { subTreeIndexReg := maxItem.index }

      stateReg := Mux(Components.swapper.io.req.ready, Mux(firstChild(maxItem.index) >= sizeReg, State.Idle, State.IssueFetchDown), State.WaitForSwapperDown)

      Components.memory.io.read.index := tailIndex
    }
  }


}

object HeapEmitter extends App {
  emitVerilog(new Heap(Heap.Parameters(4*4096, 8, 16)))
}