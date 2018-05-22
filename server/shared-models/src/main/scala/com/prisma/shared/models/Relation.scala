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

  lazy val modelAColumn: String = manifestation match {
    case Some(m: RelationTableManifestation) =>
      m.modelAColumn
    case Some(m: InlineRelationManifestation) =>
      if (m.inTableOfModelId == modelAId) getModelA_!.idField_!.dbName else m.referencingColumn
    case None =>
      "A"
  }

  lazy val modelBColumn: String = manifestation match {
    case Some(m: RelationTableManifestation) =>
      m.modelBColumn
    case Some(m: InlineRelationManifestation) =>
      if (m.inTableOfModelId == modelBId && !isSameModelRelation) getModelB_!.idField_!.dbName else m.referencingColumn
    case None =>
      "B"
  }

  lazy val hasManifestation: Boolean                                = manifestation.isDefined
  lazy val isInlineRelation: Boolean                                = manifestation.exists(_.isInstanceOf[InlineRelationManifestation])
  lazy val inlineManifestation: Option[InlineRelationManifestation] = manifestation.collect { case x: InlineRelationManifestation => x }

  // note: defaults to modelAField to handle same model, same field relations
  lazy val isSameFieldSameModelRelation: Boolean = getModelAField == getModelBField.orElse(getModelAField)

  lazy val isManyToMany: Boolean = {
    val modelAFieldIsList = getModelAField.map(_.isList).getOrElse(true)
    val modelBFieldIsList = getModelBField.map(_.isList).getOrElse(true)
    modelAFieldIsList && modelBFieldIsList
  }

  lazy val bothSidesCascade: Boolean     = modelAOnDelete == OnDelete.Cascade && modelBOnDelete == OnDelete.Cascade
  lazy val getModelA: Option[Model]      = schema.getModelById(modelAId)
  lazy val getModelA_! : Model           = getModelA.get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))
  lazy val getModelB: Option[Model]      = schema.getModelById(modelBId)
  lazy val getModelB_! : Model           = getModelB.get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))
  lazy val getModelAField: Option[Field] = modelFieldFor(modelAId, RelationSide.A)
  lazy val getModelBField: Option[Field] = modelFieldFor(modelBId, RelationSide.B)

  private def modelFieldFor(modelId: String, relationSide: RelationSide.Value): Option[Field] = {
    for {
      model <- schema.getModelById(modelId)
      field <- model.relationFieldForIdAndSide(relationId = relationTableName, relationSide = relationSide)
    } yield field
  }

  def columnForRelationSide(relationSide: RelationSide.Value): String = if (relationSide == RelationSide.A) modelAColumn else modelBColumn

  def getFieldOnModel(modelId: String): Option[Field] = {
    if (modelId == modelAId) {
      getModelAField
    } else if (modelId == modelBId) {
      getModelBField
    } else {
      sys.error(s"The model id ${modelId} is not part of this relation ${name}")
    }
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
}
