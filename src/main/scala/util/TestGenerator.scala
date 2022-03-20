package util

import scala.io.Source

object TestGenerator extends App {

  val n = args(args.indexOf("-n") + 1).toInt
  val w = args(args.indexOf("-w") + 1).toInt
  val target = args(args.indexOf("-o") + 1)

  val testSeq = Seq.tabulate(n)(i => BigInt(i + 1)).reverse //Seq.fill(n)(BigInt(w-1, scala.util.Random)+6)

  println(s"Output: $target\nnr. of values: $n\nwidth: $w")

  writeHexSeqToFile(testSeq, target)

}
object GetTestMinimum extends App {
  val source = Source.fromFile(args.head)
  val testSeq = source.getLines().map(BigInt(_, 16)).toArray
  println(s"Test file: ${args.head}\nMinimum: ${testSeq.min}\nExpected output: 0b${(testSeq.min & 0x7FF).toString(2).reverse.padTo(15,"0").reverse.mkString("")}")
}