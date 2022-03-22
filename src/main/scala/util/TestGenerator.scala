package util

import scala.io.Source

object TestGenerator extends App {

  val n = args(args.indexOf("-n") + 1).toInt
  val w = args(args.indexOf("-w") + 1).toInt
  val target = args(args.indexOf("-o") + 1)

  val testSeq = Seq.fill(n)(BigInt(w-1, scala.util.Random) + 0xF)

  println(s"Output: $target\nnr. of values: $n\nwidth: $w\nfirst element: ${testSeq.head}\nmin: ${testSeq.min}\nmax: ${testSeq.max}")

  writeHexSeqToFile(testSeq, target)

}
