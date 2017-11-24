package cool.graph.deploy.migration

import cool.graph.shared.models._

trait MigrationStepsProposer {
  def propose(current: Project, desired: Project, renames: Renames): MigrationSteps
}

object MigrationStepsProposer {
  def apply(): MigrationStepsProposer = {
    apply((current, desired, renames) => MigrationStepsProposerImpl(current, desired, renames).evaluate())
  }

  def apply(fn: (Project, Project, Renames) => MigrationSteps): MigrationStepsProposer = new MigrationStepsProposer {
    override def propose(current: Project, desired: Project, renames: Renames): MigrationSteps = fn(current, desired, renames)
  }
}

case class Renames(
    models: Map[String, String],
    enums: Map[String, String],
    fields: Map[String, String]
) {
  def getOldModelName(model: String): String = models.getOrElse(model, model)

  def getOldEnumNames(enum: String): String = enums.getOrElse(enum, enum)

  def getOldFieldName(model: String, field: String) = fields.getOrElse(s"$model.$field", field)
}

case class MigrationStepsProposerImpl(current: Project, desired: Project, renames: Renames) {
  def evaluate(): MigrationSteps = {
    MigrationSteps(modelsToCreate ++ modelsToDelete ++ fieldsToCreate ++ fieldsToDelete)
  }

  val modelsToCreate: Vector[CreateModel] = {
    for {
      model   <- desired.models.toVector
      oldName = renames.getOldModelName(model.name)
      if current.getModelByName(oldName).isEmpty
    } yield CreateModel(model.name)
  }

  val modelsToDelete: Vector[DeleteModel] = {
    for {
      currentModel <- current.models.toVector
      oldName      = renames.getOldModelName(currentModel.name)
      if desired.getModelByName(oldName).isEmpty
    } yield DeleteModel(currentModel.name)
  }

  val modelsToUpdate: Vector[UpdateModel] = {
    for {
      model   <- desired.models.toVector
      oldName = renames.getOldModelName(model.name)
      if current.getModelByName(oldName).isDefined
      if model.name != oldName
    } yield UpdateModel(name = oldName, newName = model.name)
  }

  val fieldsToCreate: Vector[CreateField] = {
    for {
      desiredModel        <- desired.models.toVector
      oldName             = renames.getOldModelName(desiredModel.name)
      currentModel        <- current.getModelByName(oldName).toVector
      fieldOfDesiredModel <- desiredModel.fields.toVector
      oldFieldName        = renames.getOldFieldName(desiredModel.name, fieldOfDesiredModel.name)
      if currentModel.getFieldByName(oldFieldName).isEmpty
    } yield {
      CreateField(
        model = desiredModel.name,
        name = fieldOfDesiredModel.name,
        typeName = fieldOfDesiredModel.typeIdentifier.toString,
        isRequired = fieldOfDesiredModel.isRequired,
        isList = fieldOfDesiredModel.isList,
        isUnique = fieldOfDesiredModel.isUnique,
        defaultValue = fieldOfDesiredModel.defaultValue.map(_.toString),
        relation = None,
        enum = None
      )
    }

  }

  val fieldsToDelete: Vector[DeleteField] = {
    for {
      newModel            <- desired.models.toVector
      currentModel        <- current.getModelByName(newModel.name).toVector
      fieldOfCurrentModel <- currentModel.fields.toVector
      if newModel.getFieldByName(fieldOfCurrentModel.name).isEmpty
    } yield DeleteField(model = newModel.name, name = fieldOfCurrentModel.name)
  }
}
