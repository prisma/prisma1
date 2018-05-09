package com.prisma.deploy.connector

import com.prisma.shared.models.TypeIdentifier.TypeIdentifier
import com.prisma.shared.models._

sealed trait DeployMutaction

//  new project deploy mutactions

case class CreateProject(projectId: String)  extends DeployMutaction
case class TruncateProject(project: Project) extends DeployMutaction
case class DeleteProject(projectId: String)  extends DeployMutaction

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

case class CreateInlineRelation(projectId: String, model: Model, field: Field, references: Model, column: String) extends DeployMutaction
