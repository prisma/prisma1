package cool.graph.util.performance

trait TimeHelper {
  def time[R](measurementName: String = "")(block: => R): R = {
    val t0           = System.nanoTime()
    val result       = block
    val t1           = System.nanoTime()
    val diffInMicros = (t1 - t0) / 1000
    val millis       = diffInMicros.toDouble / 1000
    println(s"Elapsed time [$measurementName]: ${millis}ms")
    result
  }
}
