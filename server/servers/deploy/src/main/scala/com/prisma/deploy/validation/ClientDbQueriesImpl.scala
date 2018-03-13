package com.prisma.deploy.validation

import com.prisma.deploy.DeployDependencies
import com.prisma.shared.models.{Field, Model, Project}

import scala.concurrent.Future

trait ClientDbQueries {
  def existsByModel(modelName: String): Future[Boolean]
  def existsByRelation(relationId: String): Future[Boolean]
  def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]
  def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean]
}

case class ClientDbQueriesImpl(project: Project)(implicit val dependencies: DeployDependencies) extends ClientDbQueries {

  val clientDatabase = dependencies.clientDb

  def existsByModel(modelName: String): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsByModel(project.id, modelName)
    clientDatabase.run(readOnlyBoolean(query)).map(_.head).recover { case _: java.sql.SQLSyntaxErrorException => false }
  }

  def existsByRelation(relationId: String): Future[Boolean] = {
    val query = DatabaseQueryBuilder.existsByRelation(project.id, relationId)
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

  private def readOnlyBoolean(query: SQLActionBuilder): SqlStreamingAction[Vector[Boolean], Boolean, Read] = query.as[Boolean]
}

object EmptyClientDbQueries extends ClientDbQueries {
  override def existsByModel(modelName: String): Future[Boolean]                              = Future.successful(false)
  override def existsByRelation(relationId: String): Future[Boolean]                          = Future.successful(false)
  override def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]   = Future.successful(false)
  override def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean] = Future.successful(false)
}

object FullClientDbQueries extends ClientDbQueries {
  override def existsByModel(modelName: String): Future[Boolean]                              = Future.successful(true)
  override def existsByRelation(relationId: String): Future[Boolean]                          = Future.successful(true)
  override def existsNullByModelAndScalarField(model: Model, field: Field): Future[Boolean]   = Future.successful(true)
  override def existsNullByModelAndRelationField(model: Model, field: Field): Future[Boolean] = Future.successful(true)
}
