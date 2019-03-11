package com.prisma.deploy.connector.mongo.impl

import com.prisma.deploy.connector.ClientDbQueries
import com.prisma.deploy.connector.mongo.database.MongoDeployDatabaseQueryBuilder
import com.prisma.shared.models.RelationSide.RelationSide
import com.prisma.shared.models._
import org.mongodb.scala.MongoClient
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Aggregates.{`match`, group, limit, project => mongoProjection}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._

import scala.concurrent.{ExecutionContext, Future}

case class MongoClientDbQueries(project: Project, clientDatabase: MongoClient)(implicit ec: ExecutionContext) extends ClientDbQueries {
  val database = project.dbName

  def existsByModel(model: Model): Future[Boolean] = MongoDeployDatabaseQueryBuilder.existsByModel(clientDatabase, database, model)

  def existsByRelation(relation: Relation): Future[Boolean] = {
//    val query = MongoDeployDatabaseQueryBuilder.existsByRelation(project.id, relationId)
    Future.successful(false)
  }

  def existsDuplicateByRelationAndSide(relation: Relation, relationSide: RelationSide): Future[Boolean] = {
//    val query = MongoDeployDatabaseQueryBuilder.existsDuplicateByRelationAndSide(project.id, relationId, relationSide)
    Future.successful(false)
  }

  def existsNullByModelAndField(model: Model, field: Field): Future[Boolean] = {
//    val query = field match {
//      case f: ScalarField   => MongoDeployDatabaseQueryBuilder.existsNullByModelAndScalarField(project.id, model.name, f.name)
//      case f: RelationField => MongoDeployDatabaseQueryBuilder.existsNullByModelAndRelationField(project.id, model.name, f)
//    }
    Future.successful(false)
  }

  def existsDuplicateValueByModelAndField(model: Model, field: ScalarField): Future[Boolean] = {
    clientDatabase
      .getDatabase(database)
      .getCollection(model.dbName)
      .aggregate(
        Seq(
          `match`(notEqual(field.dbName, null)),
          group(s"$$${field.dbName}", sum("count", 1)),
          `match`(gt("count", 1)),
          mongoProjection(include("_id")),
          limit(1)
        )
      )
      .toFuture()
      .map(_.nonEmpty)
  }

  override def enumValueIsInUse(models: Vector[Model], enumName: String, value: String): Future[Boolean] = {
//    val query = MongoDeployDatabaseQueryBuilder.enumValueIsInUse(project.id, models, enumName, value)
    Future.successful(false)
  }

}
