package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._

import scala.concurrent.{ExecutionContext, Future}

case class JdbcClientDbQueries(project: Project, slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcBase with ClientDbQueries {
  val queryBuilder = JdbcDeployDatabaseQueryBuilder(slickDatabase)

  def existsByModel(modelName: String): Future[Boolean] = {
    val model = project.schema.getModelByName_!(modelName)
    val query = queryBuilder.existsByModel(project.id, model)

    val result = database.run(query).recover { case _: java.sql.SQLSyntaxErrorException => false }
    result.onComplete(x => println(s"ClientDbQueries: $result"))
    result
  }

  def existsDuplicateByRelationAndSide(relationId: String, relationSide: RelationSide): Future[Boolean] = {
    val query = queryBuilder.existsDuplicateByRelationAndSide(project.id, relationId, relationSide)

    val result = database.run(query).recover { case _: java.sql.SQLSyntaxErrorException => false }
    result.onComplete(x => println(s"ClientDbQueries: $result"))
    result
  }

  def existsByRelation(relationId: String): Future[Boolean] = {
    val query = queryBuilder.existsByRelation(project.id, relationId)

    val result = database.run(query).recover { case _: java.sql.SQLSyntaxErrorException => false }
    result.onComplete(x => println(s"ClientDbQueries: $result"))
    result
  }

  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean] = {
    val query = field match {
      case f: ScalarField   => queryBuilder.existsNullByModelAndScalarField(project.id, model, f.dbName)
      case f: RelationField => queryBuilder.existsNullByModelAndRelationField(project.id, model, f)
    }

    val result = database.run(query).recover { case _: java.sql.SQLSyntaxErrorException => false }
    result.onComplete(x => println(s"ClientDbQueries: $result"))
    result
  }

  def existsDuplicateValueByModelAndField(model: Model, field: ScalarField): Future[Boolean] = {
    val query = queryBuilder.existsDuplicateValueByModelAndField(project.id, model, field.dbName)

    val result = database.run(query).recover { case _: java.sql.SQLSyntaxErrorException => false }
    result.onComplete(x => println(s"ClientDbQueries: $result"))
    result
  }

  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean] = {
    val query = queryBuilder.enumValueIsInUse(project.id, models, enumName, value)

    val result = database.run(query).recover { case _: java.sql.SQLSyntaxErrorException => false }
    result.onComplete(x => println(s"ClientDbQueries: $result"))
    result
  }
}
