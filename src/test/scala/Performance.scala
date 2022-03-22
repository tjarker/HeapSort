
import chisel3._
import HeapTest.HeapWrapper
import chiseltest._

import java.io.{File, PrintWriter}

/*
Builds a performance matrix for multiples n and k value combinations and outputs it as a markdown table
 */
object Performance extends App {

  val ks = (1 to 5).map(scala.math.pow(2, _).toInt)
  val ns = (5 to 13) map (scala.math.pow(2, _).toInt)

  val matrix = ks.map { k =>
    ns.map { n =>
      val params = Heap.Parameters(n, k, 32)
      val values = Seq.fill(params.n)(BigInt(params.w, scala.util.Random))
      var insertCycles: Option[Seq[Int]] = None
      var removeCycles: Option[Seq[Int]] = None

      println(params)
      RawTester.test(new Heap(params)) { dut =>
        insertCycles = Some(values.map(v => dut.insert(v.U)).filter(_ > 1))
        removeCycles = Some(values.sorted.reverse.map { v =>
          val (cycles, value) = dut.removeRoot()
          cycles
        }.filter(_ > 1))
      }

      (insertCycles.get.sum / insertCycles.get.length, removeCycles.get.sum / removeCycles.get.length)
    }
  }

  val out = new StringBuilder
  out.append("|       |" + ns.map(v => " n=%-4d".format(v)).mkString(" | ") + "\n")
  out.append("| :-    |" + ns.map(_ => "  :-:  ").mkString(" | ") + "\n")
  out.append(matrix.zip(ks).map { case (row, k) => "| k=%-3d |".format(k) + row.map(v => "%3d/%-3d".format(v._1, v._2)).mkString(" | ") }.mkString("\n"))
  out.append("\n\nformat: avg. number of cycles needed for {insertion}/{removal}")

  val writer = new PrintWriter(new File("doc/CycleTable.md"))
  writer.write(out.toString())
  writer.close()

  println(out.toString())

}
