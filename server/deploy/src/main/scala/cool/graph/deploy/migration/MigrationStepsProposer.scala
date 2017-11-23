package cool.graph.deploy.migration

import cool.graph.shared.models._

trait MigrationStepsProposer {
  def propose(current: Project, desired: Project): MigrationSteps
}

object MigrationStepsProposer {
  def apply(): MigrationStepsProposer = {
    apply((current, desired) => MigrationStepsProposerImpl(current, desired).evaluate())
  }

  def apply(fn: (Project, Project) => MigrationSteps): MigrationStepsProposer = new MigrationStepsProposer {
    override def propose(current: Project, desired: Project): MigrationSteps = fn(current, desired)
  }
}

case class MigrationStepsProposerImpl(current: Project, desired: Project) {
  def evaluate(): MigrationSteps = {
    MigrationSteps(modelsToCreate ++ modelsToDelete ++ fieldsToCreate ++ fieldsToDelete)
  }

  val modelsToCreate: Vector[CreateModel] = {
    for {
      model <- desired.models.toVector
      if current.getModelByName(model.name).isEmpty
    } yield CreateModel(model.name)
  }

  val modelsToDelete: Vector[DeleteModel] = {
    for {
      currentModel <- current.models.toVector
      if desired.getModelByName(currentModel.name).isEmpty
    } yield DeleteModel(currentModel.name)
  }

  val fieldsToCreate: Vector[CreateField] = {
    for {
      newModel        <- desired.models.toVector
      currentModel    <- current.getModelByName(newModel.name).toVector
      fieldOfNewModel <- newModel.fields.toVector
      if currentModel.getFieldByName(fieldOfNewModel.name).isEmpty
    } yield {
//      CreateField(
//        model = newModel.name,
//        name = fieldOfNewModel.name,
//        typeName = fieldOfNewModel.,
//        isRequired = null,
//        isList = null,
//        isUnique = null,
//        defaultValue = null
//      )
      ???
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
