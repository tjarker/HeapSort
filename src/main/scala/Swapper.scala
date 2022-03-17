
import HeapSorter.HeapParameters
import Swapper.State
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

object Swapper {

  class Request(params: HeapParameters) extends Bundle {
    import params._
    val values = Vec(2, Indexed(UInt(w.W)))
    val valid = Bool()
  }
  class Response extends Bundle {
    val done = Bool()
  }

  object State extends ChiselEnum {
    val Idle, WriteFirst, WriteSecond = Value
  }

}

class Swapper(params: HeapParameters) extends Module {
  import params._

  val io = IO(new Bundle {
    val req = Input(new Swapper.Request(params))
    val res = Output(new Swapper.Response)
    val mem = new Memory.WriteAccess(params)
  })

  val stateReg = RegInit(State.Idle)
  val valuesReg = Reg(Vec(2, Indexed(UInt(w.W))))

  io.res.done := 0.B

  switch(stateReg) {
    is(State.Idle) {
      stateReg := Mux(io.req.valid, State.WriteFirst, State.Idle)
      valuesReg := io.req.values
      io.res.done := 1.B
    }
    is(State.WriteFirst) {
      stateReg := State.WriteSecond

      io.mem.index := valuesReg(0).index
      io.mem.value := valuesReg(1).item
      io.mem.valid := 1.B
    }
    is(State.WriteSecond) {
      stateReg := State.Idle

      io.mem.index := valuesReg(1).index
      io.mem.value := valuesReg(0).item
      io.mem.valid := 1.B
    }
  }

}
