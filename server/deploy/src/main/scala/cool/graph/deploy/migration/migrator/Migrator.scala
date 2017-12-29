package cool.graph.deploy.migration.migrator

import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.shared.models.{Migration, MigrationStep, Project}
import akka.pattern.pipe

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Migrator {
  def schedule(nextProject: Project, steps: Vector[MigrationStep]): Future[Migration]
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

// How to retry failed migrations?
// How to handle graceful shutdown
//    Unwatch, stop message, wait for completion?

object Initialize
case class Schedule(nextProject: Project, steps: Vector[MigrationStep])

case class DeploymentSchedulerActor()(implicit val migrationPersistence: MigrationPersistence) extends Actor with Stash {
  implicit val dispatcher = context.system.dispatcher
  val projectWorkers      = new mutable.HashMap[String, ActorRef]()

  // Spins up new project deployment actors if a new one arrives
  // Signals deployment actors of new deployments
  //    - PubSub?
  // Enhancement(s): In the shared cluster we might face issues with too many project actors / high overhead during bootup
  //    - We could have a last active timestamp or something and if a limit is reached we reap project actors.

  def receive: Receive = {
    case Initialize =>
      val initSender = sender()
      initialize().onComplete {
        case Success(_) =>
          initSender ! akka.actor.Status.Success(())
          context.become(ready)
          unstashAll()

        case Failure(err) =>
          initSender ! akka.actor.Status.Failure(err)
          context.stop(self)
      }

    case _ =>
      stash()
  }

  def ready: Receive = {
    case Schedule(nextProject, steps) => scheduleMigration(nextProject, steps)
    case Terminated(watched)          => handleTerminated(watched)
  }

  def initialize(): Future[Unit] = {
    // Todo init logic
    // Load project actors for unapplied migration projects

    Future.successful(())
  }

  def scheduleMigration(nextProject: Project, steps: Vector[MigrationStep]) = {
    val workerRef = projectWorkers.get(nextProject.id) match {
      case Some(worker) => worker
      case None         => workerForProject(nextProject.id)
    }

    workerRef.tell(ScheduleInternal(nextProject, steps), sender())
  }

  def workerForProject(projectId: String): ActorRef = {
    val newWorker = context.actorOf(Props(ProjectDeploymentActor(projectId, 0)))

    context.watch(newWorker)
    projectWorkers += (projectId -> newWorker)
    newWorker
  }

  def handleTerminated(watched: ActorRef) = {
    projectWorkers.find(_._2 == watched) match {
      case Some((pid, _)) =>
        println(s"[Warning] Worker for project $pid terminated abnormally. Recreating...")
        workerForProject(pid)

      case None =>
        println(s"[Warning] Terminated child actor $watched has never been mapped to a project.")
    }
  }
}

case class ScheduleInternal(nextProject: Project, steps: Vector[MigrationStep])
object WorkoffDeployment
object ResumeMessageProcessing

// Todo only saves for now, doesn't work off (that is still in the applier job!)
case class ProjectDeploymentActor(projectID: String, var lastRevision: Int)(implicit val migrationPersistence: MigrationPersistence) extends Actor {
  implicit val ec = context.system.dispatcher

  // Loads last unapplied / applied migration
  // Inactive until signal
  // Possible enhancement: Periodically scan the DB for migrations if signal was lost?

  def receive: Receive = {
    case ScheduleInternal(nextProject, steps) =>
      migrationPersistence.create(nextProject, Migration(nextProject, steps)) pipeTo sender()

    case WorkoffDeployment =>
    // work off replaces the actor behaviour until the messages has been processed, as it is async and we need
    // to keep message processing sequential and consistent, but async for best performance
//      context.become {
//        case _                       =>
//        case ResumeMessageProcessing => context.unbecome()
//      }
  }
}
