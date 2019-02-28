package com.prisma.deploy.connector

import com.prisma.shared.models._

sealed trait DeployMutaction {
  def project: Project
}

case class TruncateProject(project: Project) extends DeployMutaction { val projectId = project.id }

case class CreateColumn(project: Project, model: Model, field: ScalarField)                           extends DeployMutaction
case class DeleteColumn(project: Project, model: Model, field: ScalarField)                           extends DeployMutaction
case class UpdateColumn(project: Project, model: Model, oldField: ScalarField, newField: ScalarField) extends DeployMutaction

case class CreateScalarListTable(project: Project, model: Model, field: ScalarField)                                               extends DeployMutaction
case class DeleteScalarListTable(project: Project, model: Model, field: ScalarField)                                               extends DeployMutaction
case class UpdateScalarListTable(project: Project, oldModel: Model, newModel: Model, oldField: ScalarField, newField: ScalarField) extends DeployMutaction

case class CreateModelTable(project: Project, model: Model)                                                             extends DeployMutaction
case class DeleteModelTable(project: Project, model: Model, nameOfIdField: String, scalarListFields: Vector[String])    extends DeployMutaction // delete/truncate collection based on migrations setting in server config
case class RenameTable(project: Project, previousName: String, nextName: String, scalarListFieldsNames: Vector[String]) extends DeployMutaction // rename based on migration setting

case class CreateRelationTable(project: Project, relation: Relation)                                 extends DeployMutaction
case class DeleteRelationTable(project: Project, relation: Relation)                                 extends DeployMutaction // based on migration settings;  set relation fields to null in document
case class UpdateRelationTable(project: Project, previousRelation: Relation, nextRelation: Relation) extends DeployMutaction

case class CreateInlineRelation(project: Project, relation: Relation, model: Model, references: Model, column: String) extends DeployMutaction
case class DeleteInlineRelation(project: Project, relation: Relation, model: Model, references: Model, column: String) extends DeployMutaction
case class UpdateInlineRelation(project: Project, previous: Relation, next: Relation)                                  extends DeployMutaction
