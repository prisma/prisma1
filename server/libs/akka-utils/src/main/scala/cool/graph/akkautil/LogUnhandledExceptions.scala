package cool.graph.akkautil

import akka.actor.Actor
import cool.graph.bugsnag.{BugSnagger, MetaData}

trait LogUnhandledExceptions extends Actor {

  val bugsnag: BugSnagger

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    bugsnag.report(reason, Seq(MetaData("Akka", "message", message)))
  }
}
