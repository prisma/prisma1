package cool.graph.deploy.migration

import akka.actor.{Actor, ActorRef}
import cool.graph.shared.models.Migration
import akka.actor.Stash
import cool.graph.messagebus.PubSub

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Migrator {
  def schedule(migration: Migration): Unit
}

// - Revision is an atomic sequence?
// - Always increment... but how? -> schedule actually saves the migration instead the top level thread
//   - This ensures that the single actor can serialize db access and check revision increment.
//
//- Each project has an own worker (Actor)
//-
//- Hm, we want to make sure that everything is received and in order
//- Protocol issue? ACK required?
//- Actors can make a failsafe query to ensure that the migration they get
//- ^ OR it just loads all projects and initializes deployment workers for each, the actors themselves can query the db and work off unapplied migrations
//- High spike in DB load, lots of IO on the actors, possibly overwhelming the db for smaller instances? But then again there shouldn’t be that many projects on a small instance
//
//
//- schedule on the Migrator signals the respective worker -> pubsub on projectID
//- Causes the worker to scan and send a message to self
//- Might also be a forwarding actor that does that (query + forward)
//-
//
//- LastRevisionSeen as a safety net, no need to query really, just during init

// Q: Are messages that are not matched discarded? How to store these? Look at the pattern
object Initialize
case class InitializationFailed(err: Throwable)

object Ready

case class Schedule(migration: Migration)

case class DeploymentSchedulerActor(pubSub: PubSub[String]) extends Actor with Stash {
  implicit val dispatcher = context.system.dispatcher
  val projectWorkers      = new mutable.HashMap[String, ActorRef]()

  // Spins up new project deployment actors if a new one arrives
  // Signals deployment actors of new deployments
  //    - PubSub?
  // Enhancement(s): In the shared cluster we might face issues with too many project actors / high overhead during bootup
  //    - We could have a last active timestamp or something and if a limit is reached we reap project actors.

  def receive: Receive = {
    case Initialize =>
      initialize().onComplete {
        case Success(_) =>
          println("Deployment worker initialization complete.")
          sender() ! Ready
          context.become(ready)
          unstashAll()

        case Failure(err) =>
          println(s"Deployment worker initialization failed with: $err")
          sender() ! InitializationFailed(err)
          context.stop(self)
      }

    case _ =>
      stash()
  }

  def ready: Receive = {
    case Schedule(migration) =>
  }

  def initialize(): Future[Unit] = {
    // Watch child actors and restarts if necessary
    // Load project actors for unapplied migration projects
    //

    ???
  }
}

case class ProjectDeploymentActor(projectID: String, var lastRevision: Int) extends Actor {
  // Loads last unapplied / applied migration
  // Inactive until signal
  // Possible enhancement: Periodically scan the DB for migrations if signal was lost?

  def receive = ???
}
