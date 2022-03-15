import chisel3._

object Types {

  class MemoryIO(addressWidth: Int) extends Bundle {
    val readAddress = Input(UInt(addressWidth.W))
    val readValue = Output(UInt(32.W))
    val writeAddress = Input(UInt(addressWidth.W))
    val writeValue = Input(UInt(32.W))
    val writeEnable = Input(Bool())
  }

}
