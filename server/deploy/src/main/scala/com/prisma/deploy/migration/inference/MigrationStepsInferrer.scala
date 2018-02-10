package com.prisma.deploy.migration.inference

import com.prisma.shared.models._

trait MigrationStepsInferrer {
  def infer(previousSchema: Schema, nextSchema: Schema, renames: SchemaMapping): Vector[MigrationStep]
}

object MigrationStepsInferrer {
  def apply(): MigrationStepsInferrer = {
    apply((previous, next, renames) => MigrationStepsInferrerImpl(previous, next, renames).evaluate())
  }

  def apply(fn: (Schema, Schema, SchemaMapping) => Vector[MigrationStep]): MigrationStepsInferrer = new MigrationStepsInferrer {
    override def infer(previousSchema: Schema, nextSchema: Schema, renames: SchemaMapping): Vector[MigrationStep] = fn(previousSchema, nextSchema, renames)
  }
}

case class MigrationStepsInferrerImpl(previousSchema: Schema, nextSchema: Schema, renames: SchemaMapping) {
  import com.prisma.util.Diff._

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
      nextModel         <- nextSchema.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      if previousSchema.getModelByName(previousModelName).isEmpty
    } yield CreateModel(nextModel.name)
  }

  lazy val modelsToUpdate: Vector[UpdateModel] = {
    for {
      nextModel         <- nextSchema.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      if previousSchema.getModelByName(previousModelName).isDefined
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
      previousModel <- previousSchema.models.toVector.filterNot(m => updatedModels.contains(m.name))
      if nextSchema.getModelByName(previousModel.name).isEmpty
    } yield DeleteModel(previousModel.name)
  }

  lazy val fieldsToCreate: Vector[CreateField] = {
    for {
      nextModel         <- nextSchema.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      previousModel     = previousSchema.getModelByName(previousModelName).getOrElse(emptyModel)
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
      nextModel         <- nextSchema.models.toVector
      previousModelName = renames.getPreviousModelName(nextModel.name)
      previousModel     = previousSchema.getModelByName(previousModelName).getOrElse(emptyModel)
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
      previousModel <- previousSchema.models.toVector
      previousField <- previousModel.fields
      nextModelName = renames.getNextModelName(previousModel.name)
      nextFieldName = renames.getNextFieldName(previousModel.name, previousField.name)
      nextModel     <- nextSchema.getModelByName(nextModelName)
      if nextSchema.getFieldByName(nextModelName, nextFieldName).isEmpty
    } yield DeleteField(model = nextModel.name, name = previousField.name)
  }

  lazy val relationsToCreate: Vector[CreateRelation] = {
    for {
      nextRelation <- nextSchema.relations.toVector
      if !containsRelation(previousSchema, ambiguityCheck = nextSchema, nextRelation, renames.getPreviousModelName)
    } yield {
      CreateRelation(
        name = nextRelation.name,
        modelAName = nextRelation.modelAId,
        modelBName = nextRelation.modelBId,
        modelAOnDelete = nextRelation.modelAOnDelete,
        modelBOnDelete = nextRelation.modelBOnDelete
      )
    }
  }

  lazy val relationsToDelete: Vector[DeleteRelation] = {
    for {
      previousRelation <- previousSchema.relations.toVector
      if !containsRelation(nextSchema, ambiguityCheck = previousSchema, previousRelation, renames.getNextModelName)
    } yield DeleteRelation(previousRelation.name)
  }

  lazy val relationsToUpdate: Vector[UpdateRelation] = {
    val updates = for {
      previousRelation <- previousSchema.relations.toVector
      nextModelAName   = renames.getNextModelName(previousRelation.modelAId)
      nextModelBName   = renames.getNextModelName(previousRelation.modelBId)
      nextRelation <- nextSchema // TODO: this needs to be adapted once we allow rename of relations
                       .getRelationByName(previousRelation.name)
                       .orElse(nextSchema.getUnambiguousRelationThatConnectsModels_!(nextModelAName, nextModelBName))
    } yield {
      UpdateRelation(
        name = previousRelation.name,
        newName = diff(previousRelation.name, nextRelation.name),
        modelAId = diff(previousRelation.modelAId, nextRelation.modelAId),
        modelBId = diff(previousRelation.modelBId, nextRelation.modelBId),
        modelAOnDelete = diff(previousRelation.modelAOnDelete, nextRelation.modelAOnDelete),
        modelBOnDelete = diff(previousRelation.modelBOnDelete, nextRelation.modelBOnDelete)
      )
    }
    updates.filter(isAnyOptionSet)
  }

  lazy val enumsToCreate: Vector[CreateEnum] = {
    for {
      nextEnum         <- nextSchema.enums.toVector
      previousEnumName = renames.getPreviousEnumName(nextEnum.name)
      if !containsEnum(previousSchema, previousEnumName)
    } yield CreateEnum(nextEnum.name, nextEnum.values)
  }

  lazy val enumsToDelete: Vector[DeleteEnum] = {
    for {
      previousEnum <- previousSchema.enums.toVector
      nextEnumName = renames.getNextEnumName(previousEnum.name)
      if nextSchema.getEnumByName(nextEnumName).isEmpty
    } yield DeleteEnum(previousEnum.name)
  }

  lazy val enumsToUpdate: Vector[UpdateEnum] = {
    val updates = for {
      previousEnum <- previousSchema.enums.toVector
      nextEnumName = renames.getNextEnumName(previousEnum.name)
      nextEnum     <- nextSchema.getEnumByName(nextEnumName)
    } yield {
      UpdateEnum(
        name = previousEnum.name,
        newName = diff(previousEnum.name, nextEnum.name),
        values = diff(previousEnum.values, nextEnum.values)
      )
    }
    updates.filter(isAnyOptionSet)
  }

  lazy val emptyModel = Model(name = "", stableIdentifier = "", fields = List.empty)

  def containsRelation(schema: Schema, ambiguityCheck: Schema, relation: Relation, adjacentModelName: String => String): Boolean = {
    schema.relations.exists { rel =>
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
      val relationIsUnambiguous      = rel.isUnambiguous(ambiguityCheck)

      (relationNameMatches || relationIsUnambiguous) && (refersToModelsExactlyRight || refersToModelsSwitched)
    }
  }

  def containsEnum(schema: Schema, enumName: String): Boolean = schema.enums.exists(_.name == enumName)

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
