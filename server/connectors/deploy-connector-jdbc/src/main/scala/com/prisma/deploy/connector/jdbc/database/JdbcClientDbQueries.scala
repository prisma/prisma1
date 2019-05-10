package com.prisma.deploy.connector.jdbc.database

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class JdbcClientDbQueries(project: Project, slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcBase with ClientDbQueries {

  val queryBuilder = JdbcDeployDatabaseQueryBuilder(slickDatabase)

  def existsByModel(model: Model): Future[Boolean] = {
    val query = queryBuilder.existsByModel(project.id, model)

    runAttached(query).recover(recoverPf)
  }

  def existsDuplicateByRelationAndSide(relation: Relation, relationSide: RelationSide): Future[Boolean] = {
    val query = queryBuilder.existsDuplicateByRelationAndSide(project.id, relation, relationSide)

    runAttached(query).recover(recoverPf)
  }

  def existsByRelation(relation: Relation): Future[Boolean] = {
    val query = queryBuilder.existsByRelation(project.id, relation)

    runAttached(query).recover(recoverPf)
  }

  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean] = {
    val query = field match {
      case f: ScalarField   => queryBuilder.existsNullByModelAndScalarField(project.id, model, f.dbName)
      case f: RelationField => queryBuilder.existsNullByModelAndRelationField(project.id, model, f)
    }

    runAttached(query).recover(recoverPf)
  }

  def existsDuplicateValueByModelAndField(model: Model, field: ScalarField): Future[Boolean] = {
    val query = queryBuilder.existsDuplicateValueByModelAndField(project.id, model, field.dbName)

    runAttached(query).recover(recoverPf)
  }

  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean] = {
    val query = queryBuilder.enumValueIsInUse(project.id, models, enumName, value)
    runAttached(query).recover(recoverPf)
  }

  private val recoverPf: PartialFunction[Throwable, Boolean] = {
    case _: java.sql.SQLException => false
  }

  private def runAttached[T](query: DBIO[T]) = {
    if (slickDatabase.isSQLite) {
      import slickDatabase.profile.api._
      val projectId          = project.id
      val list               = sql"""PRAGMA database_list;""".as[(String, String, String)]
      val path               = s"""'db/$projectId.db'"""
      val attach             = sqlu"ATTACH DATABASE #${path} AS #${projectId};"
      val activateForeignKey = sqlu"""PRAGMA foreign_keys = ON;"""

      val attachIfNecessary = for {
        attachedDbs <- list
        _ <- attachedDbs.map(_._2).contains(projectId) match {
              case true  => slick.dbio.DBIO.successful(())
              case false => attach
            }
        _      <- activateForeignKey
        result <- query
      } yield result

      database.run(attachIfNecessary.withPinnedSession)
    } else {
      database.run(query)
    }
  }
}
