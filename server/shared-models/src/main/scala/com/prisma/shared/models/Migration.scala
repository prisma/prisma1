package com.prisma.shared.models

import com.prisma.shared.models.MigrationStatus.MigrationStatus
import org.joda.time.DateTime

case class MigrationId(projectId: String, revision: Int)

case class Migration(
    projectId: String,
    revision: Int,
    schema: Schema,
    functions: Vector[Function],
    status: MigrationStatus,
    applied: Int,
    rolledBack: Int,
    steps: Vector[MigrationStep],
    errors: Vector[String],
    startedAt: Option[DateTime] = None,
    finishedAt: Option[DateTime] = None
) {
  def id: MigrationId                             = MigrationId(projectId, revision)
  def isRollingBack: Boolean                      = status == MigrationStatus.RollingBack
  def pendingSteps: Vector[MigrationStep]         = steps.drop(applied)
  def appliedSteps: Vector[MigrationStep]         = steps.take(applied)
  def pendingRollBackSteps: Vector[MigrationStep] = appliedSteps.reverse.drop(rolledBack)
  def currentStep: MigrationStep                  = steps(applied)
  def incApplied: Migration                       = copy(applied = applied + 1)
  def incRolledBack: Migration                    = copy(rolledBack = rolledBack + 1)
  def markAsRollBackFailure: Migration            = copy(status = MigrationStatus.RollbackFailure)
}

object MigrationStatus extends Enumeration {
  type MigrationStatus = Value

  val Pending         = Value("PENDING")
  val InProgress      = Value("IN_PROGRESS")
  val Success         = Value("SUCCESS")
  val RollingBack     = Value("ROLLING_BACK")
  val RollbackSuccess = Value("ROLLBACK_SUCCESS")
  val RollbackFailure = Value("ROLLBACK_FAILURE")

  val openStates  = Vector(Pending, InProgress, RollingBack)
  val finalStates = Vector(Success, RollbackSuccess, RollbackFailure)
}

object Migration {
  def apply(projectId: String, schema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Migration = Migration(
    projectId,
    revision = 0,
    schema = schema,
    functions = functions,
    status = MigrationStatus.Pending,
    applied = 0,
    rolledBack = 0,
    steps,
    errors = Vector.empty
  )

  def empty(projectId: String) = apply(projectId, Schema(), Vector.empty, Vector.empty)
}

sealed trait MigrationStep
sealed trait ModelMigrationStep extends MigrationStep

case class CreateModel(name: String)                  extends ModelMigrationStep
case class DeleteModel(name: String)                  extends ModelMigrationStep
case class UpdateModel(name: String, newName: String) extends ModelMigrationStep

sealed trait FieldMigrationStep extends MigrationStep
case class CreateField(
    model: String,
    name: String,
    typeName: String,
    isRequired: Boolean,
    isList: Boolean,
    isUnique: Boolean,
    relation: Option[String],
    defaultValue: Option[String],
    enum: Option[String]
) extends FieldMigrationStep

case class DeleteField(model: String, name: String) extends FieldMigrationStep

case class UpdateField(
    model: String,
    newModel: String,
    name: String,
    newName: Option[String],
    typeName: Option[String],
    isRequired: Option[Boolean],
    isList: Option[Boolean],
    isUnique: Option[Boolean],
    isHidden: Option[Boolean],
    relation: Option[Option[String]],
    defaultValue: Option[Option[String]],
    enum: Option[Option[String]]
) extends FieldMigrationStep {
  def finalName = newName.getOrElse(name)
}

sealed trait EnumMigrationStep                                                               extends MigrationStep
case class CreateEnum(name: String, values: Seq[String])                                     extends EnumMigrationStep
case class DeleteEnum(name: String)                                                          extends EnumMigrationStep
case class UpdateEnum(name: String, newName: Option[String], values: Option[Vector[String]]) extends EnumMigrationStep

object OnDelete extends Enumeration {
  type OnDelete = Value

  val SetNull = Value("SET_NULL")
  val Cascade = Value("CASCADE")

  val default = SetNull
}

sealed trait RelationMigrationStep extends MigrationStep
case class CreateRelation(
    name: String,
    modelAName: String,
    modelBName: String,
    modelAOnDelete: OnDelete.Value,
    modelBOnDelete: OnDelete.Value
) extends RelationMigrationStep

case class UpdateRelation(
    name: String,
    newName: Option[String],
    modelAId: Option[String],
    modelBId: Option[String],
    modelAOnDelete: Option[OnDelete.Value],
    modelBOnDelete: Option[OnDelete.Value]
) extends RelationMigrationStep

case class DeleteRelation(
    name: String
) extends MigrationStep

case class UpdateSecrets(secrets: Vector[String]) extends MigrationStep
