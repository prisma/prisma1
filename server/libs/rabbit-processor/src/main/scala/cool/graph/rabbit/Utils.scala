package cool.graph.rabbit

import java.text.SimpleDateFormat
import java.util.{Date, UUID}
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

object Utils {
  def timestamp: String = {
    val formatter = new SimpleDateFormat("HH:mm:ss.SSS-dd.MM.yyyy")
    val now       = new Date()
    formatter.format(now)
  }

  def timestampWithRandom: String = timestamp + "-" + UUID.randomUUID()

  def newNamedThreadFactory(name: String): ThreadFactory = new ThreadFactory {
    val count = new AtomicLong(0)

    override def newThread(runnable: Runnable): Thread = {
      val thread = new Thread(runnable)
      thread.setName(s"$name-" + count.getAndIncrement)
      thread.setDaemon(true)
      thread
    }
  }
}
