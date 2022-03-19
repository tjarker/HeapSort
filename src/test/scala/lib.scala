import chisel3.internal.firrtl.Width

object lib {

  def uRand(w: Width): BigInt = BigInt(w.get, scala.util.Random)
  def uRand(r: Range): BigInt = BigInt(r.min + scala.util.Random.nextInt(r.max - r.min))
  def uRands(w: Width, ws: Width*): Seq[BigInt] = (w +: ws).map(uRand)
  def uRands(n: Int, w: Width): Seq[BigInt] = Seq.fill(n)(uRand(w))


}
