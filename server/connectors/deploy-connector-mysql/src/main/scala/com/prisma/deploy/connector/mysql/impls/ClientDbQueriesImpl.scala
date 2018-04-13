package com.prisma.deploy.connector.mysql.impls

import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.connector.mysql.database.DatabaseQueryBuilder
import com.prisma.shared.models.{Field, Model, Project}
import slick.dbio.Effect.Read
import slick.jdbc.SQLActionBuilder
import slick.sql.SqlStreamingAction
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class ClientDbQueriesImpl(project: Project, clientDatabase: Database)(implicit ec: ExecutionContext) extends ClientDbQueries {

  def existsByModel(modelName: String): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsByModel(project.id, modelName)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsByRelation(relationId: String): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsByRelation(project.id, relationId)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean] = {
    val query = field.isScalar match {
      case true  => DatabaseQueryBuilder.existsNullByModelAndScalarField(project.id, model.name, field.name)
      case false => DatabaseQueryBuilder.existsNullByModelAndRelationField(project.id, model.name, field)
    }
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean] = {
    val query = DatabaseQueryBuilder.enumValueIsInUse(project.id, models, enumName, value)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  private def readOnlyBoolean(query: SQLActionBuilder): SqlStreamingAction[Vector[Boolean], Boolean, Read] = query.as[Boolean]
}
