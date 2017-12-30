package cool.graph.deploy.migration.migrator

import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import cool.graph.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import cool.graph.deploy.migration.MigrationApplier
import cool.graph.deploy.schema.DeploymentInProgress
import cool.graph.shared.models.{Migration, MigrationStep, Project}

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Migrator {
  def schedule(nextProject: Project, steps: Vector[MigrationStep]): Future[Migration]
}

object Initialize
case class Schedule(nextProject: Project, steps: Vector[MigrationStep])

case class DeploymentSchedulerActor()(
    implicit val migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
    applier: MigrationApplier
) extends Actor
    with Stash {
  implicit val dispatcher = context.system.dispatcher
  val projectWorkers      = new mutable.HashMap[String, ActorRef]()

  // Enhancement(s): In the shared cluster we might face issues with too many project actors / high overhead during bootup
  //    - We could have a last active timestamp or something and if a limit is reached we reap project actors.
  // How to handle graceful shutdown? -> Unwatch, stop message, wait for completion?

  def receive: Receive = {
    case Initialize =>
      val caller = sender()
      initialize().onComplete {
        case Success(_) =>
          caller ! akka.actor.Status.Success(())
          context.become(ready)
          unstashAll()

        case Failure(err) =>
          caller ! akka.actor.Status.Failure(err)
          context.stop(self)
      }

    case _ =>
      stash()
  }

  def ready: Receive = {
    case msg: Schedule       => scheduleMigration(msg)
    case Terminated(watched) => handleTerminated(watched)
  }

  def initialize(): Future[Unit] = {
    projectPersistence.loadProjectsWithUnappliedMigrations().transformWith {
      case Success(projects) => Future { projects.foreach(project => workerForProject(project.id)) }
      case Failure(err)      => Future.failed(err)
    }
  }

  def scheduleMigration(scheduleMsg: Schedule): Unit = {
    val workerRef = projectWorkers.get(scheduleMsg.nextProject.id) match {
      case Some(worker) => worker
      case None         => workerForProject(scheduleMsg.nextProject.id)
    }

    workerRef.tell(scheduleMsg, sender())
  }

  def workerForProject(projectId: String): ActorRef = {
    val newWorker = context.actorOf(Props(ProjectDeploymentActor(projectId)))

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

object ResumeMessageProcessing
object Ready
object Deploy

// Todo only saves for now, doesn't work off (that is still in the applier job!)
/**
  * State machine states:
  *  - Initializing: Stashing all messages while initializing
  *  - Ready: Ready to schedule deployments and deploy
  *  - Busy: Currently deploying or scheduling, subsequent scheduling is rejected
  *
  * Transitions: Initializing -> Ready <-> Busy
  *
  * Why a state machine? Deployment should leverage futures for optimal performance, but there should only be one deployment
  * at a time for a given project and stage. Hence, processing is kicked off async and the actor changes behavior to reject
  * scheduling and deployment until the async processing restored the ready state.
  */
case class ProjectDeploymentActor(projectId: String)(
    implicit val migrationPersistence: MigrationPersistence,
    applier: MigrationApplier
) extends Actor
    with Stash {

  implicit val ec                  = context.system.dispatcher
  var currentProjectState: Project = _ // Latest valid project, saves a DB roundtrip

  // Possible enhancement: Periodically scan the DB for migrations if signal was lost -> Wait and see if this is an issue at all

  initialize()

  def receive: Receive = {
    case Ready =>
      context.become(ready)
      unstashAll()

    case _ =>
      stash()
  }

  def ready: Receive = {
    case msg: Schedule =>
      println(s"[Debug] Scheduling deployment for project $projectId")
      val caller = sender()
      context.become(busy) // Block subsequent scheduling and deployments
      handleScheduling(msg).onComplete {
        case Success(migration: Migration) =>
          caller ! migration
          self ! Deploy // will be stashed
          self ! ResumeMessageProcessing

        case Failure(err) =>
          self ! ResumeMessageProcessing
          caller ! akka.actor.Status.Failure(err)
      }

    case Deploy =>
      context.become(busy)
      handleDeployment().onComplete {
        case Success(_) =>
          println(s"[Debug] Applied migration for project $projectId")
          self ! ResumeMessageProcessing

        case Failure(err) =>
          println(s"[Debug] Error during deployment for project $projectId: $err")
          self ! ResumeMessageProcessing // todo Mark migration as failed
      }

    // How to get migration progress into the picture?
    // How to retry? -> No retry for now? Yes. Just fail the deployment with the new migration progress.
  }

  def busy: Receive = {
    case _: Schedule =>
      sender() ! akka.actor.Status.Failure(DeploymentInProgress)

    case ResumeMessageProcessing =>
      context.become(ready)
      unstashAll()

    case x =>
      stash()
  }

  def initialize() = {
    println(s"[Debug] Initializing deployment worker for $projectId")
    migrationPersistence.getNextMigration(projectId).onComplete {
      case Success(migrationOpt) =>
        migrationOpt match {
          case Some(_) =>
            println(s"[Debug] Found unapplied migration for $projectId during init.")
            self ! Ready
            self ! Deploy

          case None =>
            self ! Ready
        }

      case Failure(err) =>
        println(s"Deployment worker initialization for project $projectId failed with $err")
        context.stop(self)
    }
  }

  def handleScheduling(msg: Schedule): Future[Migration] = {
    // Check if scheduling is possible (no pending migration), then create and return the migration
    migrationPersistence
      .getNextMigration(projectId)
      .transformWith {
        case Success(pendingMigrationOpt) =>
          pendingMigrationOpt match {
            case Some(pendingMigration) => Future.failed(DeploymentInProgress)
            case None                   => Future.unit
          }

        case Failure(err) =>
          Future.failed(err)
      }
      .flatMap { _ =>
        migrationPersistence.create(msg.nextProject, Migration(msg.nextProject, msg.steps))
      }
  }

  def handleDeployment(): Future[Unit] = {
    migrationPersistence.getUnappliedMigration(projectId).transformWith {
      case Success(Some(unapplied)) =>
        applier.applyMigration(unapplied.previousProject, unapplied.nextProject, unapplied.migration).map { result =>
          if (result.succeeded) {
            migrationPersistence.markMigrationAsApplied(unapplied.migration)
          } else {
            // todo or mark it as failed here?
            Future.failed(new Exception("Applying migration failed."))
          }
        }

      case Failure(err) =>
        Future.failed(new Exception(s"Error while fetching unapplied migration: $err"))

      case Success(None) =>
        println("[Warning] Deployment signalled but no unapplied migration found. Nothing to see here.")
        Future.unit
    }
  }
}
