package com.prisma.shared.models

import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Manifestations.{InlineRelationManifestation, RelationManifestation, RelationTableManifestation}
import scala.language.implicitConversions

case class RelationTemplate(
    name: String,
    // BEWARE: if the relation looks like this: val relation = Relation(id = "relationId", modelAId = "userId", modelBId = "todoId")
    // then the relationSide for the fields have to be "opposite", because the field's side is the side of _the other_ model
    // val userField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.B)
    // val todoField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.A)
    modelAId: Id,
    modelBId: Id,
    modelAOnDelete: OnDelete.Value,
    modelBOnDelete: OnDelete.Value,
    manifestation: Option[RelationManifestation]
) {
  def build(schema: Schema) = new Relation(this, schema)

  def connectsTheModels(model1: String, model2: String): Boolean = (modelAId == model1 && modelBId == model2) || (modelAId == model2 && modelBId == model1)

  def isSameModelRelation: Boolean = modelAId == modelBId
}

object Relation {
  implicit def asRelationTemplate(relation: Relation): RelationTemplate = relation.template
}

class Relation(
    val template: RelationTemplate,
    val schema: Schema
) {
  import template._

  lazy val relationTableName = manifestation // TODO: put this into a more readable pattern match
    .collect {
      case m: RelationTableManifestation  => m.table
      case m: InlineRelationManifestation => schema.getModelById_!(m.inTableOfModelId).dbName
    }
    .getOrElse("_" + name)

  def modelAColumn: String = manifestation match {
    case Some(m: RelationTableManifestation) =>
      m.modelAColumn
    case Some(m: InlineRelationManifestation) =>
      if (m.inTableOfModelId == modelAId) getModelA_!.idField_!.dbName else m.referencingColumn
    case None =>
      "A"
  }

  def modelBColumn: String = manifestation match {
    case Some(m: RelationTableManifestation) =>
      m.modelBColumn
    case Some(m: InlineRelationManifestation) =>
      if (m.inTableOfModelId == modelBId && !isSameModelRelation) getModelB_!.idField_!.dbName else m.referencingColumn
    case None =>
      "B"
  }

  def columnForRelationSide(relationSide: RelationSide.Value): String = if (relationSide == RelationSide.A) modelAColumn else modelBColumn

  def hasManifestation: Boolean = manifestation.isDefined
  def isInlineRelation: Boolean = manifestation.exists(_.isInstanceOf[InlineRelationManifestation])

  def inlineManifestation: Option[InlineRelationManifestation] = manifestation.collect { case x: InlineRelationManifestation => x }

  def isSameFieldSameModelRelation: Boolean = {
    // note: defaults to modelAField to handle same model, same field relations
    getModelAField(schema) == getModelBField(schema).orElse(getModelAField(schema))
  }

  def isManyToMany: Boolean = {
    val modelAFieldIsList = getModelAField(schema).map(_.isList).getOrElse(true)
    val modelBFieldIsList = getModelBField(schema).map(_.isList).getOrElse(true)
    modelAFieldIsList && modelBFieldIsList
  }

  def getFieldOnModel(modelId: String): Option[Field] = {
    if (modelId == modelAId) {
      getModelAField(schema)
    } else if (modelId == modelBId) {
      getModelBField(schema)
    } else {
      sys.error(s"The model id ${modelId} is not part of this relation ${name}")
    }
  }

  def getModelA: Option[Model] = schema.getModelById(modelAId)
  def getModelA_! : Model      = getModelA.get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))

  def getModelB: Option[Model] = schema.getModelById(modelBId)
  def getModelB_! : Model      = getModelB.get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))

  def getModelAField(schema: Schema): Option[Field] = modelFieldFor(schema, modelAId, RelationSide.A)
  def getModelBField(schema: Schema): Option[Field] = modelFieldFor(schema, modelBId, RelationSide.B)

  private def modelFieldFor(schema: Schema, modelId: String, relationSide: RelationSide.Value): Option[Field] = {
    for {
      model <- schema.getModelById(modelId)
      field <- model.relationFieldForIdAndSide(relationId = relationTableName, relationSide = relationSide)
    } yield field
  }

  def sideOfModelCascades(model: Model): Boolean = {
    if (model.id == modelAId) {
      modelAOnDelete == OnDelete.Cascade
    } else if (model.id == modelBId) {
      modelBOnDelete == OnDelete.Cascade
    } else {
      sys.error(s"The model ${model.name} is not part of the relation $name")
    }
  }

  def bothSidesCascade: Boolean = modelAOnDelete == OnDelete.Cascade && modelBOnDelete == OnDelete.Cascade
}
