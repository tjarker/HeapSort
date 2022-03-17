import chisel3._


object HeapSorter {
  case class HeapParameters(
                             n: Int, // Maximum number of elements
                             k: Int, // Order of the heap
                             w: Int, // item width
                           )
}

class HeapSorter extends Module {

  val io = IO(new Bundle {

  })

}
