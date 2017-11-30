package cool.graph.shared.models

case class UnappliedMigration(
    previousProject: Project,
    nextProject: Project,
    migration: Migration
)

case class Migration(
    projectId: String,
    revision: Int,
    hasBeenApplied: Boolean,
    steps: Vector[MigrationStep]
)

object Migration {
  val empty = Migration("", 0, hasBeenApplied = false, steps = Vector.empty)
}

sealed trait MigrationStep

sealed trait ProjectMigrationStep extends MigrationStep
sealed trait ModelMigrationStep   extends MigrationStep

case class SetupProject() extends ProjectMigrationStep

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
