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
  import cool.graph.util.Diff._

  def evaluate(): MigrationSteps = {
    MigrationSteps(modelsToCreate ++ modelsToDelete ++ modelsToUpdate ++ fieldsToCreate ++ fieldsToDelete ++ fieldsToUpdate)
  }

  lazy val modelsToCreate: Vector[CreateModel] = {
    for {
      model   <- desired.models.toVector
      oldName = renames.getOldModelName(model.name)
      if current.getModelByName(oldName).isEmpty
    } yield CreateModel(model.name)
  }

  lazy val modelsToDelete: Vector[DeleteModel] = {
    for {
      currentModel <- current.models.toVector
      oldName      = renames.getOldModelName(currentModel.name)
      if desired.getModelByName(oldName).isEmpty
    } yield DeleteModel(currentModel.name)
  }

  lazy val modelsToUpdate: Vector[UpdateModel] = {
    for {
      model   <- desired.models.toVector
      oldName = renames.getOldModelName(model.name)
      if current.getModelByName(oldName).isDefined
      if model.name != oldName
    } yield UpdateModel(name = oldName, newName = model.name)
  }

  lazy val fieldsToCreate: Vector[CreateField] = {
    for {
      desiredModel        <- desired.models.toVector
      oldName             = renames.getOldModelName(desiredModel.name)
      currentModel        = current.getModelByName(oldName).getOrElse(emptyModel)
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

  lazy val fieldsToUpdate: Vector[UpdateField] = {
    val tmp = for {
      desiredModel        <- desired.models.toVector
      oldName             = renames.getOldModelName(desiredModel.name)
      currentModel        = current.getModelByName(oldName).getOrElse(emptyModel)
      fieldOfDesiredModel <- desiredModel.fields.toVector
      oldFieldName        = renames.getOldFieldName(desiredModel.name, fieldOfDesiredModel.name)
      currentField        <- currentModel.getFieldByName(oldFieldName)
    } yield {
      UpdateField(
        model = oldName,
        name = oldFieldName,
        newName = diff(oldName, desiredModel.name),
        typeName = diff(currentField.typeIdentifier.toString, fieldOfDesiredModel.typeIdentifier.toString),
        isRequired = diff(currentField.isRequired, fieldOfDesiredModel.isRequired),
        isList = diff(currentField.isList, fieldOfDesiredModel.isList),
        isUnique = diff(currentField.isUnique, fieldOfDesiredModel.isUnique),
        relation = diff(currentField.relation.map(_.id), fieldOfDesiredModel.relation.map(_.id)),
        defaultValue = diff(currentField.defaultValue, fieldOfDesiredModel.defaultValue).map(_.map(_.toString)),
        enum = diff(currentField.enum, fieldOfDesiredModel.enum).map(_.map(_.id))
      )
    }
    tmp.filter(isAnyOptionSet)
  }

  lazy val fieldsToDelete: Vector[DeleteField] = {
    for {
      newModel            <- desired.models.toVector
      oldName             = renames.getOldModelName(newModel.name)
      currentModel        <- current.getModelByName(oldName).toVector
      fieldOfCurrentModel <- currentModel.fields.toVector
      oldFieldName        = renames.getOldFieldName(oldName, fieldOfCurrentModel.name)
      if newModel.getFieldByName(oldFieldName).isEmpty
    } yield DeleteField(model = newModel.name, name = fieldOfCurrentModel.name)
  }

  lazy val emptyModel = Model(
    id = "",
    name = "",
    fields = List.empty,
    description = None,
    isSystem = false,
    permissions = List.empty,
    fieldPositions = List.empty
  )

  def isAnyOptionSet(product: Product): Boolean = {
    import shapeless._
    import syntax.typeable._
    product.productIterator.exists { value =>
      value.cast[Option[Any]] match {
        case Some(x) => x.isDefined
        case None    => false
      }
    }
  }
}
