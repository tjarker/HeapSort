
import chisel3._
import HeapTest.HeapWrapper
import Performance.args
import chiseltest._

import java.io.{File, PrintWriter}
import scala.io.Source

/*
Builds a performance matrix for multiples n and k value combinations and outputs it as a markdown table
 */
object Performance extends App {

  val ks = (1 to 7).map(scala.math.pow(2, _).toInt)
  val files = new File("test-files").listFiles(_.isFile)

  val writer = new PrintWriter(new File(args.head))

  writer.println(s"k, n, w, order, insertion, removal")

  val matrix = ks.map { k =>
    files.sorted.map { file =>

      val source = Source.fromFile(file)
      val values = source.getLines().map(BigInt(_, 16)).toArray
      source.close()

      val n = values.length
      val order = if(file.getName.contains("reverse")) "reverse" else if(file.getName.contains("sorted")) "sorted" else "random"

      print(s"$k, $n, 32, $order, ")
      writer.print(s"$k, $n, 32, $order, ")

      val params = Heap.Parameters(n, k, 32)

      RawTester.test(new Heap(params)) { dut =>
        val insertion = values.map(v => dut.insert(v.U)).sum
        print(s"$insertion, ")
        writer.print(s"$insertion, ")
        val removal = values.sorted.reverse.map(_ => dut.removeRoot()._1).sum
        println(removal)
        writer.println(removal)
      }
    }
  }

  writer.close()

}


object PowerFileParser extends App {

  val files = new File("energy").listFiles(_.isFile)

  println(files.mkString("Array(", ", ", ")"))

  val powers = files.map { file =>
    val source = Source.fromFile(file)
    val number = source.getLines().filter {
      s => s.contains("Total On-Chip Power (W)")
    }.map {s =>
      "\\d*\\.\\d*".r.findFirstIn(s).get
    }.toArray.head
    source.close()
    val Seq(n,k) = "[0-9]+".r.findAllIn(file.getName).toSeq
    (k.toInt,n.toInt,number)
  }

  println(powers.sortBy(_._1.toInt).mkString("\n"))
  val writer = new PrintWriter(new File("heap-sort-power.csv"))
  writer.println("k,n,power")
  writer.println(powers.sorted.map { case (k,n,power) => s"$k, ${n * 1024}, $power"}.mkString("\n"))
  writer.close()
}