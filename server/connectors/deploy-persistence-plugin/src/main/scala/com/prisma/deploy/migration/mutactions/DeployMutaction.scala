package com.prisma.deploy.migration.mutactions

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models.{Field, Model, Relation, Schema}

import scala.concurrent.Future

sealed trait DeployMutaction

// those should be named fields
case class CreateColumn(projectId: String, model: Model, field: Field)                     extends DeployMutaction
case class DeleteColumn(projectId: String, model: Model, field: Field)                     extends DeployMutaction
case class UpdateColumn(projectId: String, model: Model, oldField: Field, newField: Field) extends DeployMutaction

case class CreateScalarListTable(projectId: String, model: String, field: String, typeIdentifier: TypeIdentifier)       extends DeployMutaction
case class DeleteScalarListTable(projectId: String, model: String, field: String, typeIdentifier: TypeIdentifier)       extends DeployMutaction
case class UpdateScalarListTable(projectId: String, oldModel: Model, newModel: Model, oldField: Field, newField: Field) extends DeployMutaction

case class CreateModelTable(projectId: String, model: String)                                                            extends DeployMutaction
case class DeleteModelTable(projectId: String, model: String, scalarListFields: Vector[String])                          extends DeployMutaction
case class RenameTable(projectId: String, previousName: String, nextName: String, scalarListFieldsNames: Vector[String]) extends DeployMutaction

case class CreateRelationTable(projectId: String, schema: Schema, relation: Relation) extends DeployMutaction
case class DeleteRelationTable(projectId: String, schema: Schema, relation: Relation) extends DeployMutaction

trait DeployMutactionExecutor {
  def execute(mutaction: DeployMutaction): Future[Unit]
  def rollback(mutaction: DeployMutaction): Future[Unit]
}
