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

  lazy val bothSidesCascade: Boolean                                = modelAOnDelete == OnDelete.Cascade && modelBOnDelete == OnDelete.Cascade
  lazy val modelA: Option[Model]                                    = schema.getModelById(modelAId)
  lazy val modelA_! : Model                                         = modelA.get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))
  lazy val modelB: Option[Model]                                    = schema.getModelById(modelBId)
  lazy val modelB_! : Model                                         = modelB.get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))
  lazy val modelAField: Option[Field]                               = modelFieldFor(modelAId, RelationSide.A)
  lazy val modelBField: Option[Field]                               = modelFieldFor(modelBId, RelationSide.B)
  lazy val hasManifestation: Boolean                                = manifestation.isDefined
  lazy val isInlineRelation: Boolean                                = manifestation.exists(_.isInstanceOf[InlineRelationManifestation])
  lazy val inlineManifestation: Option[InlineRelationManifestation] = manifestation.collect { case x: InlineRelationManifestation => x }
  // note: defaults to modelAField to handle same model, same field relations
  lazy val isSameFieldSameModelRelation: Boolean = modelAField == modelBField.orElse(modelAField)

  lazy val relationTableName = manifestation match {
    case Some(m: RelationTableManifestation)  => m.table
    case Some(m: InlineRelationManifestation) => schema.getModelById_!(m.inTableOfModelId).dbName
    case None                                 => "_" + name
  }

  lazy val modelAColumn: String = manifestation match {
    case Some(m: RelationTableManifestation)  => m.modelAColumn
    case Some(m: InlineRelationManifestation) => if (m.inTableOfModelId == modelAId) modelA_!.idField_!.dbName else m.referencingColumn
    case None                                 => "A"
  }

  lazy val modelBColumn: String = manifestation match {
    case Some(m: RelationTableManifestation)  => m.modelBColumn
    case Some(m: InlineRelationManifestation) => if (m.inTableOfModelId == modelBId && !isSameModelRelation) modelB_!.idField_!.dbName else m.referencingColumn
    case None                                 => "B"
  }

  lazy val isManyToMany: Boolean = {
    val modelAFieldIsList = modelAField.map(_.isList).getOrElse(true)
    val modelBFieldIsList = modelBField.map(_.isList).getOrElse(true)
    modelAFieldIsList && modelBFieldIsList
  }

  private def modelFieldFor(modelId: String, relationSide: RelationSide.Value): Option[Field] = {
    for {
      model <- schema.getModelById(modelId)
      field <- model.relationFieldForIdAndSide(relationId = relationTableName, relationSide = relationSide)
    } yield field
  }

  def columnForRelationSide(relationSide: RelationSide.Value): String = if (relationSide == RelationSide.A) modelAColumn else modelBColumn

  def getFieldOnModel(modelId: String): Option[Field] = {
    modelId match {
      case `modelAId` => modelAField
      case `modelBId` => modelBField
      case _          => sys.error(s"The model id ${modelId} is not part of this relation ${name}")
    }
  }

  def sideOfModelCascades(model: Model): Boolean = {
    model.name match {
      case `modelAId` => modelAOnDelete == OnDelete.Cascade
      case `modelBId` => modelBOnDelete == OnDelete.Cascade
      case _          => sys.error(s"The model ${model.name} is not part of the relation $name")
    }
  }
}
