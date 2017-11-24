package cool.graph.shared.models

import cool.graph.cuid.Cuid
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier

case class MigrationSteps(
    steps: Vector[MigrationStep]
)
object MigrationSteps {
  val empty = MigrationSteps(steps = Vector.empty)
}

sealed trait MigrationStep
sealed trait ModelMigrationStep                       extends MigrationStep
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
) extends FieldMigrationStep

sealed trait EnumMigrationStep                                                               extends MigrationStep
case class CreateEnum(model: String, values: Seq[String])                                    extends EnumMigrationStep
case class DeleteEnum(name: String)                                                          extends EnumMigrationStep
case class UpdateEnum(name: String, newName: Option[String], values: Option[Vector[String]]) extends EnumMigrationStep

sealed trait RelationMigrationStep extends MigrationStep
