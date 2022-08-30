import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.experimental.ChiselEnum
import chisel3.util._


object Color {
  def apply(r: Int, g: Int, b: Int): Color = {
    (new Color).Lit(_.r -> r.U, _.g -> g.U, _.b -> b.U)
  }
}

class Color extends Bundle {

  val r = UInt(8.W)
  val g = UInt(8.W)
  val b = UInt(8.W)

}


class LedController(frequency: Int) extends Module {

  object State extends ChiselEnum {
    val Idle, SendReset, SendBits = Value
  }

  val io = IO(new Bundle {

    val colors = Input(Vec(4, new Color))
    val out = Output(Bool())

  })


  val stateReg = RegInit(State.Idle)

  val bitsReg = RegInit(VecInit(Seq.fill(24 * 4)(1.B)))
  val bitCounter = RegInit(0.U(log2Ceil(24 * 4).W))
  val changeFlag = RegInit(0.B)

  val bitSender = Module(new BitSender(frequency))
  bitSender.io.in.valid := 0.B
  bitSender.io.in.bits.reset := 0.B
  bitSender.io.in.bits.data := 0.B

  io.out := bitSender.io.out


  switch(stateReg) {

    is(State.Idle) {

      val newBits = io.colors.map(c => c.g ## c.r ## c.b).reduce(_ ## _).asBools.take(24 * 4)
      bitsReg := newBits

      when(VecInit(newBits.zip(bitsReg).map { case (n,o) => n =/= o }).reduceTree(_ || _) || changeFlag) {

        when(bitSender.io.in.ready) {
          stateReg := State.SendReset
          bitSender.io.in.bits.reset := 1.B
          bitSender.io.in.valid := 1.B
          bitCounter := 0.U
        } otherwise {
          changeFlag := 1.B
        }


      }

    }

    is(State.SendReset) {
      when(bitSender.io.in.ready) {
        stateReg := State.SendBits
      }
    }

    is(State.SendBits) {

      changeFlag := 0.B

      when(bitSender.io.in.ready) {

        bitSender.io.in.bits.data := bitsReg(bitCounter)
        bitSender.io.in.valid := 1.B

        bitCounter := bitCounter + 1.U

        when(bitCounter === ((24 * 4) - 1).U) {

          stateReg := State.Idle

        }


      }


    }

  }





}