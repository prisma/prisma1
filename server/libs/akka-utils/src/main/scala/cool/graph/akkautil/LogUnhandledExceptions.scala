package cool.graph.akkautil

import akka.actor.Actor
import com.prisma.errors.{ErrorReporter, GenericMetadata}

trait LogUnhandledExceptions extends Actor {

  val reporter: ErrorReporter

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    reporter.report(reason, GenericMetadata("Akka", "Message", message.toString))
  }
}
