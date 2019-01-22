package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._

import scala.concurrent.{ExecutionContext, Future}

case class JdbcClientDbQueries(project: Project, slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcBase with ClientDbQueries {
  val queryBuilder = JdbcDeployDatabaseQueryBuilder(slickDatabase)

  def existsByModel(model: Model): Future[Boolean] = {
    val query = queryBuilder.existsByModel(project.id, model)

    database.run(query).recover(recoverPf)
  }

  def existsDuplicateByRelationAndSide(relation: Relation, relationSide: RelationSide): Future[Boolean] = {
    val query = queryBuilder.existsDuplicateByRelationAndSide(project.id, relation, relationSide)

    database.run(query).recover(recoverPf)
  }

  def existsByRelation(relation: Relation): Future[Boolean] = {
    val query = queryBuilder.existsByRelation(project.id, relation)

    database.run(query).recover(recoverPf)
  }

  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean] = {
    val query = field match {
      case f: ScalarField   => queryBuilder.existsNullByModelAndScalarField(project.id, model, f.dbName)
      case f: RelationField => queryBuilder.existsNullByModelAndRelationField(project.id, model, f)
    }

    database.run(query).recover(recoverPf)
  }

  def existsDuplicateValueByModelAndField(model: Model, field: ScalarField): Future[Boolean] = {
    val query = queryBuilder.existsDuplicateValueByModelAndField(project.id, model, field.dbName)

    database.run(query).recover(recoverPf)
  }

  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean] = {
    val query = queryBuilder.enumValueIsInUse(project.id, models, enumName, value)
    database.run(query).recover(recoverPf)
  }

  private val recoverPf: PartialFunction[Throwable, Boolean] = {
    case _: java.sql.SQLException => false
  }
}
