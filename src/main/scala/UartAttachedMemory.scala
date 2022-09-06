

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

import scala.math

class UartAttachedMemory[T <: Data](size: Int, gen: T)(frequency: Int, baudRate: Int) extends Module {


  object State extends ChiselEnum {
    val Idle, ReceiveAddress, ReceiveData, Write = Value
  }

  val io = IO(new Bundle {

    val txd = Output(Bool())
    val rxd = Input(Bool())

    val write = Input(Bool())
    val address = Input(UInt(log2Ceil(size).W))
    val writeData = Input(chiselTypeOf(gen))
    val readData = Output(chiselTypeOf(gen))

    val busy = Output(Bool())

  })

  val ADDRESS_TRANSMISSION_BEATS = math.ceil(log2Ceil(size) / 8.0).toInt
  val DATA_TRANSMISSION_BEATS = math.ceil(gen.asUInt.getWidth / 8.0).toInt

  val memory = SyncReadMem(size, chiselTypeOf(gen))
  val receiver = Module(new Rx(frequency, baudRate))
  receiver.io.channel.ready := 0.B
  val transmitter = Module(new Tx(frequency, baudRate))

  val stateReg = RegInit(State.Idle)
  val beatCounter = RegInit(UInt(log2Ceil(
    math.max(ADDRESS_TRANSMISSION_BEATS, DATA_TRANSMISSION_BEATS)
  ).W), 0.U)
  val beatCounterDec = WireDefault(0.B)
  beatCounter := Mux(beatCounterDec, beatCounter - 1.U, beatCounter)
  val allBeatsReceived = beatCounter === 0.U
  val addressReg = RegInit(UInt(log2Ceil(size).W), 0.U)
  val dataReg = RegInit(chiselTypeOf(gen), 0.U.asTypeOf(gen))


  val write = WireDefault(0.B)
  val address = WireDefault(0.U(log2Ceil(size).W))
  val writeData = Wire(chiselTypeOf(gen))
  val readData = Wire(chiselTypeOf(gen))

  address := io.address
  writeData := io.writeData

  when(write) {
    memory.write(address, writeData)
    readData := DontCare
  } otherwise {
    readData := memory.read(address)
  }

  io.readData := readData
  io.busy := stateReg =/= State.Idle

  switch(stateReg) {
    is(State.Idle) {

      write := io.write

      when(receiver.io.channel.valid) {
        beatCounter := ADDRESS_TRANSMISSION_BEATS.U
        stateReg := State.ReceiveAddress
      }


    }
    is(State.ReceiveAddress) {

      when(receiver.io.channel.valid) {
        addressReg := (addressReg << 8) ## receiver.io.channel.bits
        beatCounterDec := 1.B
      }

      when(allBeatsReceived) {
        beatCounter := DATA_TRANSMISSION_BEATS.U
        stateReg := State.ReceiveData
      } otherwise {
        receiver.io.channel.ready := 1.B
      }

    }
    is(State.ReceiveData) {

      when(receiver.io.channel.valid) {
        dataReg := ((dataReg.asUInt << 8) ## receiver.io.channel.bits).asTypeOf(gen)
        beatCounterDec := 1.B
      }

      when(allBeatsReceived) {
        stateReg := State.Write
      } otherwise {
        receiver.io.channel.ready := 1.B
      }

    }
    is(State.Write) {
      write := 1.B
      address := addressReg
      writeData := dataReg
      stateReg := State.Idle
    }
  }

}
