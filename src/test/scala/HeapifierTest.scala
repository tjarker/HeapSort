
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import lib.{uRand, uRands}

class HeapifierTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Heapifier"

  it should "please work" in {
    val params = Heap.Parameters(32, 8, 8)
    import params._

    (0 until 30).foreach { _ =>

      val root = uRand(w.W)
      val children = uRands(k, w.W)
      val index = uRand(0 until (n / k))
      val childrenIndices = Seq.tabulate(k)(i => index + 1 + i)

      def validGen(highestValid: Int): Seq[Boolean] = Seq.tabulate(k)(_ <= highestValid)

      val validLim = uRand(0 to k)
      val valid = validGen(validLim.toInt)

      val (max, maxIndex) = (root +: children)
        .zipWithIndex.map{ case (m, i) => (m, index+i) }
        .zip(true +: valid).reduce { (l, r) =>
        if (r._2 && (r._1._1 > l._1._1)) r else l
      }._1

      val childrenString = (children,childrenIndices,valid).zipped.map { case (c,i,v) => if(v) s"($i)$c" else s"($i)XX" }.mkString(" - ")
      println(s"${" " * (childrenString.length/2 - 2)}($index)$root")
      println(childrenString)
      println(s" ===> ($maxIndex)$max ${if(maxIndex == index) "(no swap)" else ""}")

      test(new Heapifier(params)) { dut =>

        dut.io.fetcher.valid.poke(1.B)
        dut.io.fetcher.parent.index.poke(index.U)
        dut.io.fetcher.parent.item.poke(root.U)
        dut.io.fetcher.children.map(_.valid).zip(valid).foreach { case (p, v) => p.poke(v.B) }
        dut.io.fetcher.children.map(_.data.index).zip(childrenIndices).foreach { case (p, v) => p.poke(v.U) }
        dut.io.fetcher.children.map(_.data.item).zip(children).foreach { case (p, v) => p.poke(v.U) }

        dut.io.res.valid.expect(0.B)

        dut.clock.step()

        if (maxIndex == index) {

          dut.io.res.valid.expect(1.B)
          dut.io.res.swapped.expect(0.B)

        } else {

          dut.io.swapper.values(0).item.expect(root.U)
          dut.io.swapper.values(0).index.expect(index.U)
          dut.io.swapper.values(1).item.expect(max.U)
          dut.io.swapper.values(1).index.expect(maxIndex.U)
          dut.io.swapper.valid.expect(1.B)

          dut.io.res.valid.expect(0.B)

          dut.clock.step()

          dut.io.swapper.ready.poke(1.B)

          dut.io.res.valid.expect(1.B)
          dut.io.res.swapped.expect(1.B)
          dut.io.res.largest.expect(maxIndex.U)

        }

      }
    }



  }
}

