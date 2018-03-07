package com.prisma.deploy.migration.mutactions

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Field, Model, Relation, Schema}

import scala.concurrent.Future
import scala.util.{Success, Try}

trait ClientSqlMutaction {
//  def verify(): Future[Try[Unit]] = Future.successful(Success(()))
//
//  def execute: Future[ClientSqlStatementResult[Any]]
//
//  def rollback: Option[Future[ClientSqlStatementResult[Any]]] = None
}

//case class ClientSqlStatementResult[A <: Any](sqlAction: DBIOAction[A, NoStream, Effect.All])

case class CreateClientDatabaseForProject(projectId: String) extends ClientSqlMutaction
case class DeleteClientDatabaseForProject(projectId: String) extends ClientSqlMutaction
// those should be named fields
case class CreateColumn(projectId: String, model: Model, field: Field)                     extends ClientSqlMutaction
case class DeleteColumn(projectId: String, model: Model, field: Field)                     extends ClientSqlMutaction
case class UpdateColumn(projectId: String, model: Model, oldField: Field, newField: Field) extends ClientSqlMutaction

case class CreateScalarListTable(projectId: String, model: String, field: String, typeIdentifier: TypeIdentifier)       extends ClientSqlMutaction
case class DeleteScalarListTable(projectId: String, model: String, field: String, typeIdentifier: TypeIdentifier)       extends ClientSqlMutaction
case class UpdateScalarListTable(projectId: String, oldModel: Model, newModel: Model, oldField: Field, newField: Field) extends ClientSqlMutaction

case class CreateModelTable(projectId: String, model: String)                                                            extends ClientSqlMutaction
case class DeleteModelTable(projectId: String, model: String, scalarListFields: Vector[String])                          extends ClientSqlMutaction
case class RenameTable(projectId: String, previousName: String, nextName: String, scalarListFieldsNames: Vector[String]) extends ClientSqlMutaction

case class CreateRelationTable(projectId: String, schema: Schema, relation: Relation) extends ClientSqlMutaction
case class DeleteRelationTable(projectId: String, schema: Schema, relation: Relation) extends ClientSqlMutaction

trait AnyMutactionExecutor {
  def execute(mutaction: ClientSqlMutaction): Future[Unit]
  def rollback(mutaction: ClientSqlMutaction): Future[Unit]
}

object FailingAnyMutactionExecutor extends AnyMutactionExecutor {
  override def execute(mutaction: ClientSqlMutaction) = Future.failed(new Exception(this.getClass.getSimpleName))

  override def rollback(mutaction: ClientSqlMutaction) = Future.failed(new Exception(this.getClass.getSimpleName))
}
