package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.messagebus.PubSubPublisher
import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.models.{Client, Project}
import cool.graph.system.database.finder.CachedProjectResolver
import cool.graph.system.database.tables.{SeatTable, Tables}
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Project schemas are cached in a shared redis instance in the system cluster:
  * - System Api + Schema Manager lives in one stack that is only deployed in ireland.
  * - Other regions have all other apis and services deployed in a client stack.
  *
  * This mutaction is only invoked by the system api / admin service.
  * It will invalidate the local redis in a blocking fashion before sending a message to the invalidation publisher.
  * that other stacks will subscribe to.
  *
  * There are at least two consumers of the invalidation message:
  * - The subscription manager that caches the schema in memory.
  * - The AutoInvalidatingProjectCache that caches sangria schemas in memory.
  */
object InvalidateSchema {
  def apply(project: Project)(implicit inj: Injector): InvalidateSchema = InvalidateSchema(project.id)
}

case class InvalidateSchema(projectId: String)(implicit inj: Injector) extends InvalidateSchemaBase {

  def projectIds: Future[Vector[String]] = Future.successful(Vector(projectId))
}

case class InvalidateAllSchemas()(implicit inj: Injector) extends InvalidateSchemaBase {
  import slick.jdbc.MySQLProfile.api._

  var invalidationCount = 0

  def projectIds: Future[Vector[String]] = {
    val query = for {
      project <- Tables.Projects
    } yield {
      project.id
    }
    internalDatabase.run(query.result).map { projectIds =>
      invalidationCount = projectIds.size
      projectIds.toVector
    }
  }
}

case class InvalidateSchemaForAllProjects(client: Client)(implicit inj: Injector) extends InvalidateSchemaBase {

  def projectIds: Future[Vector[String]] = {
    import slick.jdbc.MySQLProfile.api._
    import slick.lifted.TableQuery

    val seatFuture = internalDatabase.run(TableQuery[SeatTable].filter(_.email === client.email).result)
    seatFuture.map { seats =>
      seats.toVector.map(_.projectId)
    }
  }
}

abstract class InvalidateSchemaBase()(implicit inj: Injector) extends Mutaction with Injectable {
  val internalDatabase: DatabaseDef = inject[DatabaseDef](identified by "internal-db")
  val cachedProjectResolver         = inject[CachedProjectResolver](identified by "cachedProjectResolver")
  val invalidationPublisher         = inject[PubSubPublisher[String]](identified by "schema-invalidation-publisher")

  override def execute: Future[MutactionExecutionResult] = {
    projectIds.flatMap { projectIdsOrAliases =>
      val invalidationFutures: Seq[Future[Unit]] = projectIdsOrAliases.map(cachedProjectResolver.invalidate)

      Future.sequence(invalidationFutures).map { _ =>
        invalidate(projectIds = projectIdsOrAliases)
        MutactionExecutionSuccess()
      }
    }
  }

  private def invalidate(projectIds: Seq[String]): Unit = projectIds.foreach(pid => invalidationPublisher.publish(Only(pid), pid))
  protected def projectIds: Future[Vector[String]]
  override def rollback = Some(ClientMutactionNoop().execute)
}
