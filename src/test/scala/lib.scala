import chisel3.internal.firrtl.Width
import chisel3.util.log2Ceil

object lib {

  def uRand(w: Width): BigInt = BigInt(w.get, scala.util.Random)
  def uRand(r: Range): BigInt = BigInt(r.min + scala.util.Random.nextInt(r.max - r.min))
  def uRands(w: Width, ws: Width*): Seq[BigInt] = (w +: ws).map(uRand)
  def uRands(n: Int, w: Width): Seq[BigInt] = Seq.fill(n)(uRand(w))

  def randomParameters(): Heap.Parameters = {
    val k = scala.math.pow(2,uRand(1 until 5).toInt).toInt
    Heap.Parameters(
      scala.math.pow(2,uRand(log2Ceil(k) until 14).toInt).toInt,
      k,
      uRand(3 until 32).toInt
    )
  }

}
