package com.prisma.deploy.migration.migrator

import akka.actor.{Actor, ActorRef, Props, Stash, Terminated}
import com.prisma.deploy.connector.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.messagebus.PubSubPublisher

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class DeploymentSchedulerActor(
    migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
    deployConnector: DeployConnector,
    invalidationPublisher: PubSubPublisher[String]
) extends Actor
    with Stash {
  import DeploymentProtocol._

  implicit val dispatcher = context.system.dispatcher
  val projectWorkers      = new mutable.HashMap[String, ActorRef]()

  // Enhancement(s):
  //    - In the shared cluster we might face issues with too many project actors / high overhead during bootup
  //    - We could have a last active timestamp or something and if a limit is reached we reap project actors.
  // todo How to handle graceful shutdown? -> Unwatch, stop message, wait for completion?

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
    // Ensure that we're the only deploy agent running on the db, then resume init.
    println("Obtaining exclusive agent lock...")
    deployConnector.managementLock().flatMap { _ =>
      println("Obtaining exclusive agent lock... Successful.")
      migrationPersistence.loadDistinctUnmigratedProjectIds().transformWith {
        case Success(projectIds) => Future { projectIds.foreach(workerForProject) }
        case Failure(err)        => Future.failed(err)
      }
    }
  }

  def scheduleMigration(scheduleMsg: Schedule): Unit = {
    val workerRef = projectWorkers.get(scheduleMsg.projectId) match {
      case Some(worker) => worker
      case None         => workerForProject(scheduleMsg.projectId)
    }

    workerRef.tell(scheduleMsg, sender)
  }

  def workerForProject(projectId: String): ActorRef = {
    val newWorker = context.actorOf(Props(ProjectDeploymentActor(projectId, migrationPersistence, deployConnector, invalidationPublisher)))

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
