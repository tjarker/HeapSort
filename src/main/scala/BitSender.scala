import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

class BitSender(frequency: Long) extends Module {

  object State extends ChiselEnum {
    val Idle, SendFirst, SendSecond = Value
  }

  val io = IO(new Bundle {
    val out = Output(Bool())
    val in = Flipped(Decoupled(new Bundle {
      val data = Bool()
      val reset = Bool()
    }))
  })

  val T0H = (220 + 380) / 2
  val T0L = (580 + 1000) / 2
  val T1H = (580 + 1000) / 2
  val T1L = (220 + 420) / 2
  val TRES = 800//300000

  val C0H = (T0H * frequency) / 1000000000L
  val C0L = (T0L * frequency) / 1000000000L
  val C1H = (T1H * frequency) / 1000000000L
  val C1L = (T1L * frequency) / 1000000000L
  val CRES = (TRES * frequency) / 1000000000L

  println(C0H, C0L, C1H, C1L, CRES)

  val stateReg = RegInit(State.Idle)
  val bitReg = RegInit(0.B)
  val counter = RegInit(0.U(log2Ceil(CRES).W))
  counter := Mux(counter > 0.U, counter - 1.U, 0.U)

  io.in.ready := 0.B
  io.out := 1.B

  switch(stateReg) {

    is(State.Idle) {
      bitReg := io.in.bits.data
      io.in.ready := 1.B

      when(io.in.valid) {

        when(io.in.bits.reset) {

          counter := CRES.U
          stateReg := State.SendSecond

        } otherwise {

          counter := Mux(io.in.bits.data, C1H.U, C0H.U)
          stateReg := State.SendFirst

        }

      }
    }

    is(State.SendFirst) {

      io.out := 1.B

      when(counter === 0.U) {

        counter := Mux(bitReg, C1L.U, C0L.U)
        stateReg := State.SendSecond

      }

    }

    is(State.SendSecond) {

      io.out := 0.B

      when(counter === 0.U) {

        stateReg := State.Idle

      }

    }

  }

}