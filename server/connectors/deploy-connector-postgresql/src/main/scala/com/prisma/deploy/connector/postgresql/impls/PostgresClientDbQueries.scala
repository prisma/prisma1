package com.prisma.deploy.connector.postgresql.impls

import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.connector.postgresql.database.PostgresDeployDatabaseQueryBuilder
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models.{Field, Model, Project}
import slick.dbio.Effect.Read
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder
import slick.sql.SqlStreamingAction

import scala.concurrent.{ExecutionContext, Future}

case class PostgresClientDbQueries(project: Project, clientDatabase: Database)(implicit ec: ExecutionContext) extends ClientDbQueries {

  def existsByModel(modelName: String): Future[Boolean] = {
    val query = PostgresDeployDatabaseQueryBuilder.existsByModel(project.id, modelName)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsDuplicateByRelationAndSide(relationId: String, relationSide: RelationSide): Future[Boolean] = {
    val query = PostgresDeployDatabaseQueryBuilder.existsDuplicateByRelationAndSide(project.id, relationId, relationSide)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsByRelation(relationId: String): Future[Boolean] = {
    val query = PostgresDeployDatabaseQueryBuilder.existsByRelation(project.id, relationId)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean] = {
    val query = field.isScalar match {
      case true  => PostgresDeployDatabaseQueryBuilder.existsNullByModelAndScalarField(project.id, model.name, field.name)
      case false => PostgresDeployDatabaseQueryBuilder.existsNullByModelAndRelationField(project.id, model.name, field)
    }
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean] = {
    val query = PostgresDeployDatabaseQueryBuilder.enumValueIsInUse(project.id, models, enumName, value)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  private def readOnlyBoolean(query: SQLActionBuilder): SqlStreamingAction[Vector[Boolean], Boolean, Read] = query.as[Boolean]
}
