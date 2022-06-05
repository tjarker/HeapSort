import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util.{HasBlackBoxInline, log2Ceil}


object Rom {
  def apply(contents: Seq[BigInt], width: Width)(address: UInt): UInt = {
    val mod = Module(new Rom(contents, width))
    mod.io.address := address
    mod.io.word
  }
}

class Rom(romContents : Seq[BigInt], width: Width) extends BlackBox with HasBlackBoxInline {

  val addrWidth = log2Ceil(romContents.length)

  val io = IO(new Bundle {
    val address = Input(UInt(addrWidth.W))
    val word = Output(UInt(width))
  })

  val romLinePattern = "\t|\t%d: word = %d'h%x;"

  val HEADER =    """module %s
                    |#(
                    |    parameter  addrWidth = %d,
                    |               outputWidth = %d
                    |)
                    |(
                    |    input wire [addrWidth-1:0] address,
                    |    output reg [outputWidth-1:0] word
                    |);
        """.format(name, addrWidth, width.get.toInt)

  val FOOTER =    """
                        |    default: begin
                        |        word = %d'bx;
                        |        `ifndef SYNTHESIS
                        |            // synthesis translate_off
                        |            word = {1{$random}};
                        |            // synthesis translate_on
                        |        `endif
                        |    end
                        |endcase
        """.format(width.get.toInt)

  val FOOTER_MODULE = "\n|endmodule"


  var BODY = "\n|always @(*) case (address)\n"

  val TABLE = romContents.zipWithIndex.map { case (v, i) =>
    romLinePattern.format(i, width.get.toInt, v)
  }.mkString("\n")

  setInline(name + ".v", (HEADER + BODY + TABLE + FOOTER + FOOTER_MODULE).stripMargin)
}