package com.prisma.metrics

import akka.actor.{Actor, ActorRef}
import com.librato.metrics.client._
import com.prisma.metrics.LibratoFlushActor.Flush

import scala.collection.mutable

case class LibratoReporter(actor: ActorRef) {
  def report(measure: TaggedMeasure): Unit = actor ! measure
}

object LibratoFlushActor {
  object Flush
}

case class LibratoFlushActor(client: LibratoClient) extends Actor {
  import scala.concurrent.duration._
  implicit val ec   = context.system.dispatcher
  val flushInterval = 5.seconds

  context.system.scheduler.schedule(flushInterval, flushInterval, self, Flush)

  val measures = mutable.Buffer.empty[TaggedMeasure]

  override def receive = {
    case measure: TaggedMeasure =>
      measures += measure

    case Flush =>
      val measuresToPost = new Measures()
      measures.foreach { measure =>
        measuresToPost.add(measure)
      }
      client.postMeasures(measuresToPost)
      measures.clear()
  }
}
