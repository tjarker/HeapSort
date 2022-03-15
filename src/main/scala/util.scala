import chisel3.util.log2Ceil

import java.io.{File, PrintWriter}

object util {

  def transpose(x: Seq[Seq[Int]]): Seq[Seq[Int]] =
    Seq.tabulate(x.head.length)(i => x.map(_(i)))

  def splitIntoBanks(k: Int)(seq: Seq[Int]): Seq[Seq[Int]] = {
    val grouped = createGroups(k)(seq.tail)
    transpose(grouped :+ Seq(seq.head,0,0,0))
  }
  def getRootBankIndex(k: Int, n: Int): Int = (n-2) / k

  def createGroups(k: Int)(seq: Seq[Int]): Seq[Seq[Int]] =
    seq.grouped(k).map(_.padTo(k,0)).toSeq

  def createBuildDir(): Unit = {
    val dir = new File("build")
    if(!dir.exists()) dir.mkdir()
  }

  def writeHexSeqToFile(seq: Seq[Int], fileName: String): Unit = {
    println(seq.toList)
    val writer = new PrintWriter(new File(fileName))
    writer.write(seq.map(_.toHexString).mkString("\n"))
    writer.close()
  }

}
