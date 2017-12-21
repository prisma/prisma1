package cool.graph.deploy.migration

import cool.graph.shared.models._

trait MigrationStepsProposer {
  def propose(currentProject: Project, nextProject: Project, renames: Renames): Vector[MigrationStep]
}

object MigrationStepsProposer {
  def apply(): MigrationStepsProposer = {
    apply((current, next, renames) => MigrationStepsProposerImpl(current, next, renames).evaluate())
  }

  def apply(fn: (Project, Project, Renames) => Vector[MigrationStep]): MigrationStepsProposer = new MigrationStepsProposer {
    override def propose(currentProject: Project, nextProject: Project, renames: Renames): Vector[MigrationStep] = fn(currentProject, nextProject, renames)
  }
}

//todo This is not really tracking renames. Renames can be deducted from this mapping, but all it does is mapping previous to current values.
// TransitionMapping?
case class Renames(
    models: Vector[Rename] = Vector.empty,
    enums: Vector[Rename] = Vector.empty,
    fields: Vector[FieldRename] = Vector.empty
) {
  def getPreviousModelName(nextModel: String): String = models.find(_.next == nextModel).map(_.previous).getOrElse(nextModel)
  def getPreviousEnumName(nextEnum: String): String   = enums.find(_.next == nextEnum).map(_.previous).getOrElse(nextEnum)
  def getPreviousFieldName(nextModel: String, nextField: String): String =
    fields.find(r => r.nextModel == nextModel && r.nextField == nextField).map(_.previousField).getOrElse(nextField)

  def getNextModelName(previousModel: String): String = models.find(_.previous == previousModel).map(_.next).getOrElse(previousModel)
  def getNextEnumName(previousEnum: String): String   = enums.find(_.previous == previousEnum).map(_.next).getOrElse(previousEnum)
  def getNextFieldName(previousModel: String, previousField: String) =
    fields.find(r => r.previousModel == previousModel && r.previousField == previousField).map(_.nextField).getOrElse(previousField)
}

case class Rename(previous: String, next: String)
case class FieldRename(previousModel: String, previousField: String, nextModel: String, nextField: String)

object Renames {
  val empty = Renames()
}

// todo Doesnt propose a thing. It generates the steps, but they cant be rejected or approved. Naming is off.
case class MigrationStepsProposerImpl(previousProject: Project, nextProject: Project, renames: Renames) {
  import cool.graph.util.Diff._

  /**
    * The following evaluation order considers all interdependencies:
    * - Delete Relation
    * - Delete Field
    * - Delete Model
    * - Delete Enum
    * - Create Enum
    * - Create Model
    * - Create Field
    * - Create Relation
    * - Update Enum
    * - Update Field
    * - Update Model
    * - Update Relation
    *
    * Note that all actions can be performed on the database level without the knowledge of previous or next migration steps.
    * This would not be true if, for example, the order would be reversed, as field updates and deletes would need to know the new
    * table name instead of the old one to successfully execute their SQL statements, increasing implementation complexity
    * and having more surface area for bugs. The model shown allows to _just look at the previous project_ and apply
    * all steps, instead of knowing the next project state as well.
    */
  def evaluate(): Vector[MigrationStep] = {
    relationsToDelete ++
      fieldsToDelete ++
      modelsToDelete ++
      enumsToDelete ++
      enumsToCreate ++
      modelsToCreate ++
      fieldsToCreate ++
      relationsToCreate ++
      enumsToUpdate ++
      fieldsToUpdate ++
      modelsToUpdate ++
      relationsToUpdate
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
    val updates = for {
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
        newName = diff(previousField.name, fieldOfNextModel.name),
        typeName = diff(previousField.typeIdentifier.toString, fieldOfNextModel.typeIdentifier.toString),
        isRequired = diff(previousField.isRequired, fieldOfNextModel.isRequired),
        isList = diff(previousField.isList, fieldOfNextModel.isList),
        isUnique = diff(previousField.isUnique, fieldOfNextModel.isUnique),
        isHidden = diff(previousField.isHidden, fieldOfNextModel.isHidden),
        relation = diff(previousField.relation.map(_.id), fieldOfNextModel.relation.map(_.id)),
        defaultValue = diff(previousField.defaultValue, fieldOfNextModel.defaultValue).map(_.map(_.toString)),
        enum = diff(previousField.enum.map(_.name), fieldOfNextModel.enum.map(_.name))
      )
    }

    updates.filter(isAnyOptionSet)
  }

  lazy val fieldsToDelete: Vector[DeleteField] = {
    for {
      previousModel <- previousProject.models.toVector
      previousField <- previousModel.fields
      nextModelName = renames.getNextModelName(previousModel.name)
      nextFieldName = renames.getNextFieldName(previousModel.name, previousField.name)
      nextModel     <- nextProject.getModelByName(nextModelName)
      if nextProject.getFieldByName(nextModelName, nextFieldName).isEmpty
    } yield DeleteField(model = nextModel.name, name = previousField.name)
  }

  lazy val relationsToCreate: Vector[CreateRelation] = {
    for {
      nextRelation <- nextProject.relations.toVector
      if !containsRelation(previousProject, nextRelation, renames.getPreviousModelName)
    } yield {
      CreateRelation(
        name = nextRelation.name,
        leftModelName = nextRelation.modelAId,
        rightModelName = nextRelation.modelBId
      )
    }
  }

  lazy val relationsToDelete: Vector[DeleteRelation] = {
    for {
      previousRelation <- previousProject.relations.toVector
      if !containsRelation(nextProject, previousRelation, renames.getNextModelName)
    } yield DeleteRelation(previousRelation.name)
  }

  lazy val relationsToUpdate: Vector[UpdateRelation] = {
    for {
      previousRelation <- previousProject.relations.toVector
      nextModelAName   = renames.getNextModelName(previousRelation.modelAId)
      nextModelBName   = renames.getNextModelName(previousRelation.modelBId)
      nextRelation     <- nextProject.getRelationsThatConnectModels(nextModelAName, nextModelBName).headOption
    } yield {
      UpdateRelation(
        relation = previousRelation.name,
        name = diff(previousRelation.name, nextRelation.name),
        modelAId = diff(previousRelation.modelAId, nextRelation.modelAId),
        modelBId = diff(previousRelation.modelBId, nextRelation.modelBId)
      )
    }
  }

  lazy val enumsToCreate: Vector[CreateEnum] = {
    for {
      nextEnum         <- nextProject.enums.toVector
      previousEnumName = renames.getPreviousEnumName(nextEnum.name)
      if !containsEnum(previousProject, previousEnumName)
    } yield CreateEnum(nextEnum.name, nextEnum.values)
  }

  lazy val enumsToDelete: Vector[DeleteEnum] = {
    for {
      previousEnum <- previousProject.enums.toVector
      nextEnumName = renames.getNextEnumName(previousEnum.name)
      if nextProject.getEnumByName(nextEnumName).isEmpty
    } yield DeleteEnum(previousEnum.name)
  }

  lazy val enumsToUpdate: Vector[UpdateEnum] = {
    (for {
      previousEnum <- previousProject.enums.toVector
      nextEnumName = renames.getNextEnumName(previousEnum.name)
      nextEnum     <- nextProject.getEnumByName(nextEnumName)
    } yield {
      UpdateEnum(
        name = previousEnum.name,
        newName = diff(previousEnum.name, nextEnum.name),
        values = diff(previousEnum.values, nextEnum.values)
      )
    }).filter(isAnyOptionSet)
  }

  lazy val emptyModel = Model(
    id = "",
    name = "",
    fields = List.empty,
    description = None
  )

  def containsRelation(project: Project, relation: Relation, adjacentModelName: String => String): Boolean = {
    project.relations.exists { rel =>
      val adjacentModelAId  = adjacentModelName(relation.modelAId)
      val adajacentModelBId = adjacentModelName(relation.modelBId)
      val adjacentGeneratedRelationName = if (adjacentModelAId < adajacentModelBId) {
        s"${adjacentModelAId}To${adajacentModelBId}"
      } else {
        s"${adajacentModelBId}To${adjacentModelAId}"
      }

      val refersToModelsExactlyRight = rel.modelAId == adjacentModelAId && rel.modelBId == adajacentModelBId
      val refersToModelsSwitched     = rel.modelAId == adajacentModelBId && rel.modelBId == adjacentModelAId
      val relationNameMatches        = rel.name == adjacentGeneratedRelationName || rel.name == relation.name
      relationNameMatches && (refersToModelsExactlyRight || refersToModelsSwitched)
    }
  }

  def containsEnum(project: Project, enumName: String): Boolean = project.enums.exists(_.name == enumName)

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
