package cool.graph.deploy.migration

import cool.graph.shared.models._

trait MigrationStepsProposer {
  def propose(currentProject: Project, nextProject: Project, renames: Renames): MigrationSteps
}

object MigrationStepsProposer {
  def apply(): MigrationStepsProposer = {
    apply((current, next, renames) => MigrationStepsProposerImpl(current, next, renames).evaluate())
  }

  def apply(fn: (Project, Project, Renames) => MigrationSteps): MigrationStepsProposer = new MigrationStepsProposer {
    override def propose(currentProject: Project, nextProject: Project, renames: Renames): MigrationSteps = fn(currentProject, nextProject, renames)
  }
}

//todo This is not really tracking renames. Renames can be deducted from this mapping, but all it does is mapping previous to current values.
// TransitionMapping?
case class Renames(
    models: Map[String, String],
    enums: Map[String, String],
    fields: Map[(String, String), String]
) {
  def getPreviousModelName(model: String): String        = models.getOrElse(model, model)
  def getPreviousEnumNames(enum: String): String         = enums.getOrElse(enum, enum)
  def getPreviousFieldName(model: String, field: String) = fields.getOrElse((model, field), field)
}

object Renames {
  val empty = Renames(Map.empty, Map.empty, Map.empty)
}

// todo Doesnt propose a thing. It generates the steps, but they cant be rejected or approved. Naming is off.
case class MigrationStepsProposerImpl(previousProject: Project, nextProject: Project, renames: Renames) {
  import cool.graph.util.Diff._

  def evaluate(): MigrationSteps = {
    MigrationSteps(modelsToCreate ++ modelsToUpdate ++ modelsToDelete ++ fieldsToCreate ++ fieldsToDelete ++ fieldsToUpdate ++ relationsToCreate)
  }

  lazy val modelsToCreate: Vector[CreateModel] = {
    for {
      nextModel         <- nextProject.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      if previousProject.getModelByName(previousModelName).isEmpty
    } yield CreateModel(nextModel.name)
  }

  lazy val modelsToUpdate: Vector[UpdateModel] = {
    for {
      nextModel         <- nextProject.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      if previousProject.getModelByName(previousModelName).isDefined
      if nextModel.name != previousModelName
    } yield UpdateModel(name = previousModelName, newName = nextModel.name)
  }

  /*
   * Check all previous models if they are present on on the new one, ignore renames (== updated models).
   * Use the _previous_ model name to check presence, and as updates are ignored this has to yield a result,
   * or else the model is deleted.
   */
  lazy val modelsToDelete: Vector[DeleteModel] = {
    val updatedModels = modelsToUpdate.map(_.name)
    for {
      previousModel <- previousProject.models.toVector.filterNot(m => updatedModels.contains(m.name))
      if nextProject.getModelByName(previousModel.name).isEmpty
    } yield DeleteModel(previousModel.name)
  }

  lazy val fieldsToCreate: Vector[CreateField] = {
    for {
      nextModel         <- nextProject.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      previousModel     = previousProject.getModelByName(previousModelName).getOrElse(emptyModel)
      fieldOfNextModel  <- nextModel.fields.toVector
      previousFieldName = renames.getPreviousFieldName(nextModel.name, fieldOfNextModel.name)
      if previousModel.getFieldByName(previousFieldName).isEmpty
    } yield {
      CreateField(
        model = nextModel.name,
        name = fieldOfNextModel.name,
        typeName = fieldOfNextModel.typeIdentifier.toString,
        isRequired = fieldOfNextModel.isRequired,
        isList = fieldOfNextModel.isList,
        isUnique = fieldOfNextModel.isUnique,
        defaultValue = fieldOfNextModel.defaultValue.map(_.toString),
        relation = fieldOfNextModel.relation.map(_.name),
        enum = fieldOfNextModel.enum.map(_.name)
      )
    }
  }

  lazy val fieldsToUpdate: Vector[UpdateField] = {
    val tmp = for {
      nextModel         <- nextProject.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      previousModel     = previousProject.getModelByName(previousModelName).getOrElse(emptyModel)
      fieldOfNextModel  <- nextModel.fields.toVector
      previousFieldName = renames.getPreviousFieldName(nextModel.name, fieldOfNextModel.name)
      previousField     <- previousModel.getFieldByName(previousFieldName)
    } yield {
      UpdateField(
        model = previousModelName,
        name = previousFieldName,
        newName = diff(previousFieldName, previousFieldName),
        typeName = diff(previousField.typeIdentifier.toString, fieldOfNextModel.typeIdentifier.toString),
        isRequired = diff(previousField.isRequired, fieldOfNextModel.isRequired),
        isList = diff(previousField.isList, fieldOfNextModel.isList),
        isUnique = diff(previousField.isUnique, fieldOfNextModel.isUnique),
        relation = diff(previousField.relation.map(_.id), fieldOfNextModel.relation.map(_.id)),
        defaultValue = diff(previousField.defaultValue, fieldOfNextModel.defaultValue).map(_.map(_.toString)),
        enum = diff(previousField.enum, fieldOfNextModel.enum).map(_.map(_.id))
      )
    }

    tmp.filter(isAnyOptionSet)
  }

  lazy val fieldsToDelete: Vector[DeleteField] = {
    for {
      nextModel            <- nextProject.models.toVector
      previousModelName    = renames.getPreviousModelName(nextModel.name)
      previousModel        <- previousProject.getModelByName(previousModelName).toVector
      fieldOfPreviousModel <- previousModel.fields.toVector
      previousFieldName    = renames.getPreviousFieldName(previousModelName, fieldOfPreviousModel.name)
      if nextModel.getFieldByName(previousFieldName).isEmpty
    } yield DeleteField(model = nextModel.name, name = fieldOfPreviousModel.name)
  }

  lazy val relationsToCreate: Vector[CreateRelation] = {
    def containsRelation(project: Project, relation: Relation): Boolean = {
      project.relations.exists { rel =>
        rel.name == relation.name && rel.modelAId == relation.modelAId && rel.modelBId == relation.modelBId
      }
    }
    for {
      nextRelation <- nextProject.relations.toVector
      if !containsRelation(previousProject, nextRelation)
    } yield {
      CreateRelation(
        name = nextRelation.name,
        leftModelName = nextRelation.modelAId,
        rightModelName = nextRelation.modelBId
      )
    }
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
