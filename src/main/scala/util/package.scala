import chisel3.util.log2Ceil

import java.io.{File, PrintWriter}

package object util {

  def writeHexSeqToFile(seq: Seq[BigInt], fileName: String): Unit = {
    val writer = new PrintWriter(new File(fileName))
    writer.write(seq.map(_.toString(16)).mkString("\n"))
    writer.close()
  }

  def nextPow2(x: Int): Int = scala.math.pow(2, log2Ceil(x)).toInt

}
