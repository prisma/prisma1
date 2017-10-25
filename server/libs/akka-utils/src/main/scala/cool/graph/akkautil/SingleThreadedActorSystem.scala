package cool.graph.akkautil

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ThreadFactory}

import akka.actor.ActorSystem

object SingleThreadedActorSystem {
  def apply(name: String): ActorSystem = {
    val ec = scala.concurrent.ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor(newNamedThreadFactory(name)))
    ActorSystem(name, defaultExecutionContext = Some(ec))
  }

  def newNamedThreadFactory(name: String): ThreadFactory = new ThreadFactory {
    val count = new AtomicLong(0)

    override def newThread(runnable: Runnable): Thread = {
      val thread = new Thread(runnable)
      thread.setDaemon(true)
      thread.setName(s"$name-" + count.getAndIncrement)
      thread
    }
  }
}
