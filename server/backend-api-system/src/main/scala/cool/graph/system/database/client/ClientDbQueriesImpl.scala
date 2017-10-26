package cool.graph.system.database.client

import cool.graph.client.database.DatabaseQueryBuilder
import cool.graph.shared.database.GlobalDatabaseManager
import cool.graph.shared.models.{Field, Model, Project, Relation}
import slick.dbio.Effect.Read
import slick.jdbc.SQLActionBuilder
import slick.sql.SqlStreamingAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ClientDbQueries {
  def itemCountForModel(model: Model): Future[Int]
  def existsByModel(model: Model): Future[Boolean]
  def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]
  def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean]
  def itemCountForRelation(relation: Relation): Future[Int]
  def itemCountForFieldValue(model: Model, field: Field, enumValue: String): Future[Int]
}

case class ClientDbQueriesImpl(globalDatabaseManager: GlobalDatabaseManager)(project: Project) extends ClientDbQueries {
  val clientDatabase = globalDatabaseManager.getDbForProject(project).readOnly

  def itemCountForModel(model: Model): Future[Int] = {
    val query = DatabaseQueryBuilder.itemCountForTable(project.id, model.name)
    clientDatabase.run(readOnlyInt(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => 0 }
  }

  def existsByModel(model: Model): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsByModel(project.id, model.name)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsNullByModelAndScalarField(project.id, model.name, field.name)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsNullByModelAndRelationField(project.id, model.name, field)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def itemCountForRelation(relation: Relation): Future[Int] = {
    val query = DatabaseQueryBuilder.itemCountForTable(project.id, relation.id)
    clientDatabase.run(readOnlyInt(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => 0 }
  }

  def itemCountForFieldValue(model: Model, field: Field, enumValue: String): Future[Int] = {
    val query = DatabaseQueryBuilder.valueCountForScalarField(project.id, model.name, field.name, enumValue)
    clientDatabase.run(readOnlyInt(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => 0 }
  }

  private def readOnlyInt(query: SQLActionBuilder): SqlStreamingAction[Vector[Int], Int, Read] = {
    val action: SqlStreamingAction[Vector[Int], Int, Read] = query.as[Int]

    action
  }

  private def readOnlyBoolean(query: SQLActionBuilder): SqlStreamingAction[Vector[Boolean], Boolean, Read] = {
    val action: SqlStreamingAction[Vector[Boolean], Boolean, Read] = query.as[Boolean]

    action
  }
}

object EmptyClientDbQueries extends ClientDbQueries {
  override def existsByModel(model: Model): Future[Boolean]                                       = Future.successful(false)
  override def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]       = Future.successful(false)
  override def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean]     = Future.successful(false)
  override def itemCountForModel(model: Model): Future[Int]                                       = Future.successful(0)
  override def itemCountForFieldValue(model: Model, field: Field, enumValue: String): Future[Int] = Future.successful(0)
  override def itemCountForRelation(relation: Relation): Future[Int]                              = Future.successful(0)

}
