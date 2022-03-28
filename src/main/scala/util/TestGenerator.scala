package util

import scala.io.Source

object TestGenerator extends App {

  val n = args(args.indexOf("-n") + 1).toInt
  val w = args(args.indexOf("-w") + 1).toInt
  val target = args(args.indexOf("-o") + 1)

  val testSeq = Seq.fill(n)(BigInt(w, scala.util.Random))

  println(s"Output: $target\nnr. of values: $n\nwidth: $w\nfirst element: ${testSeq.head}\nmin: ${testSeq.min}\nmax: ${testSeq.max}")

  writeHexSeqToFile(testSeq, s"test-files/$target-random.txt")
  writeHexSeqToFile(testSeq.sorted, s"test-files/$target-sorted.txt")
  writeHexSeqToFile(testSeq.sorted.reverse, s"test-files/$target-reverse-sorted.txt")
}
