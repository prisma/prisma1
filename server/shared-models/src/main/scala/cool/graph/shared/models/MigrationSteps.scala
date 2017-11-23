package cool.graph.shared.models

case class MigrationSteps(
    steps: Vector[MigrationStep]
)
object MigrationSteps {
  val empty = MigrationSteps(steps = Vector.empty)
}

trait MigrationStep
trait ModelMigrationStep                              extends MigrationStep
case class CreateModel(name: String)                  extends ModelMigrationStep
case class DeleteModel(name: String)                  extends ModelMigrationStep
case class UpdateModel(name: String, newName: String) extends ModelMigrationStep

trait FieldMigrationStep extends MigrationStep
case class CreateField(
    model: String,
    name: String,
    typeName: String,
    isRequired: Boolean,
    isList: Boolean,
    isUnique: Boolean,
    defaultValue: Option[String]
) extends FieldMigrationStep
case class DeleteField(model: String, name: String)                              extends FieldMigrationStep
case class UpdateField(model: String, name: String, isRequired: Option[Boolean]) extends FieldMigrationStep

trait EnumMigrationStep                                                                      extends MigrationStep
case class CreateEnum(model: String, values: Seq[String])                                    extends EnumMigrationStep
case class DeleteEnum(name: String)                                                          extends EnumMigrationStep
case class UpdateEnum(name: String, newName: Option[String], values: Option[Vector[String]]) extends EnumMigrationStep

trait RelationMigrationStep extends MigrationStep
