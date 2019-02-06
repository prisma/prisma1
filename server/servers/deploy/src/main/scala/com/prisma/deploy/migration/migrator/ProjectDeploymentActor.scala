package com.prisma.deploy.migration.migrator

import akka.actor.{Actor, Stash}
import com.prisma.deploy.connector.persistence.MigrationPersistence
import com.prisma.deploy.connector.{DeployConnector, MigrationStepMapperImpl}
import com.prisma.deploy.schema.DeploymentInProgress
import com.prisma.messagebus.PubSubPublisher
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.{Function, Migration, MigrationStep, Project, Schema}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object DeploymentProtocol {
  object Initialize
  case class Schedule(project: Project, nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function], rawDataModel: String)
  object ResumeMessageProcessing
  object Ready
  object Deploy
}

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
case class ProjectDeploymentActor(
    project: Project,
    migrationPersistence: MigrationPersistence,
    deployConnector: DeployConnector,
    invalidationPublisher: PubSubPublisher[String]
) extends Actor
    with Stash {
  import DeploymentProtocol._

  implicit val ec = context.system.dispatcher
  val applier = MigrationApplierImpl(
    migrationPersistence,
    deployConnector.projectPersistence,
    MigrationStepMapperImpl(project),
    deployConnector.deployMutactionExecutor,
    deployConnector.testFacilities.inspector
  )
  var activeSchema: Schema = _

  // Possible enhancement: Periodically scan the DB for migrations if signal was lost -> Wait and see if this is an issue at all
  // Possible enhancement: Migration retry in case of transient errors.

  initialize()

  def initialize() = {
    println(s"[Debug] Initializing deployment worker for ${project.id}")
    migrationPersistence.getLastMigration(project.id).map {
      case Some(migration) =>
        activeSchema = migration.schema
        migrationPersistence.getNextMigration(project.id).onComplete {
          case Success(migrationOpt) =>
            migrationOpt match {
              case Some(_) =>
                println(s"[Debug] Found unapplied migration for ${project.id} during init.")
                self ! Ready
                self ! Deploy

              case None =>
                self ! Ready
            }

          case Failure(err) =>
            println(s"Deployment worker initialization for project ${project.id} failed with $err")
            context.stop(self)
        }

      case None =>
        println(s"Deployment worker initialization for project ${project.id} failed: No current migration found for project.")
        context.stop(self)
    }
  }

  def receive: Receive = {
    case Ready =>
      context.become(ready)
      unstashAll()

    case _ =>
      stash()
  }

  def ready: Receive = {
    case msg: Schedule =>
      println(s"[Debug] Scheduling deployment for project ${project.id}")
      val caller = sender()
      context.become(busy) // Block subsequent scheduling and deployments
      handleScheduling(msg).onComplete {
        case Success(migration: Migration) =>
          caller ! migration
          self ! Deploy
          self ! ResumeMessageProcessing

        case Failure(err) =>
          self ! ResumeMessageProcessing
          caller ! akka.actor.Status.Failure(err)
      }

    case Deploy =>
      context.become(busy)
      handleDeployment().onComplete {
        case Success(_) =>
          println(s"[Debug] Applied migration for project ${project.id}")
          self ! ResumeMessageProcessing

        case Failure(err) =>
          println(s"[Debug] Error during deployment for project ${project.id}: $err")
          self ! ResumeMessageProcessing // todo Mark migration as failed
      }
  }

  def busy: Receive = {
    case _: Schedule =>
      sender ! akka.actor.Status.Failure(DeploymentInProgress)

    case ResumeMessageProcessing =>
      context.become(ready)
      unstashAll()

    case _ =>
      stash()
  }

  def handleScheduling(msg: Schedule): Future[Migration] = {
    // Check if scheduling is possible (no pending migration), then create and return the migration
    migrationPersistence
      .getNextMigration(project.id)
      .transformWith {
        case Success(pendingMigrationOpt) =>
          pendingMigrationOpt match {
            case Some(_) => Future.failed(DeploymentInProgress)
            case None    => Future.unit
          }

        case Failure(err) =>
          Future.failed(err)
      }
      .flatMap { _ =>
        migrationPersistence.create(Migration(project.id, msg.nextSchema, msg.steps, msg.functions, msg.rawDataModel))
      }
  }

  def handleDeployment(): Future[Unit] = {
    migrationPersistence.getNextMigration(project.id).transformWith {
      case Success(Some(nextMigration)) =>
        applier.apply(project, previousSchema = activeSchema, migration = nextMigration).map { result =>
          if (result.succeeded) {
            activeSchema = nextMigration.schema
          }
          invalidationPublisher.publish(Only(project.id), project.id)
        }

      case Failure(err) =>
        Future.failed(new Exception(s"Error while fetching migration: $err"))

      case Success(None) =>
        println("[Warning] Deployment signalled but no open migration found. Nothing to see here.")
        Future.unit
    }
  }
}
