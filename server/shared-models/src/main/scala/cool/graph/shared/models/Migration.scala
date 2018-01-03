package cool.graph.shared.models

import cool.graph.shared.models.MigrationStatus.MigrationStatus

//case class UnappliedMigration(
//    previousProject: Project,
//    nextProject: Project,
//    migration: Migration
//)

case class Migration(
    projectId: String,
    revision: Int,
    schema: Schema,
    status: MigrationStatus,
    progress: Int,
    steps: Vector[MigrationStep],
    errors: Vector[String]
)

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
  def apply(projectId: String, steps: Vector[MigrationStep]): Migration = Migration(
    projectId,
    revision = 0,
    schema = Schema(),
    status = MigrationStatus.Pending,
    progress = 0,
    steps,
    errors = Vector.empty
  )

//  def empty(project: Project) = apply(project, Vector.empty)
  def empty(projectId: String) = apply(projectId, Vector.empty)
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

sealed trait RelationMigrationStep extends MigrationStep
case class CreateRelation(
    name: String,
    leftModelName: String,
    rightModelName: String
) extends RelationMigrationStep

case class DeleteRelation(
    name: String
) extends MigrationStep
