package com.prisma.shared.models

import com.prisma.shared.errors.SharedErrors
import com.prisma.shared.models.IdType.Id

object Schema {
  val empty = Schema()
}

case class Schema(
    modelTemplates: List[ModelTemplate] = List.empty,
    relationTemplates: List[RelationTemplate] = List.empty,
    enums: List[Enum] = List.empty
) {
  val models    = modelTemplates.map(_.build(this))
  val relations = relationTemplates.map(_.build(this))

  def allFields: Seq[Field] = models.flatMap(_.fields)

  def fieldsWhereThisModelIsRequired(model: Model) = allFields.filter(f => f.isRequired && !f.isList && f.relatedModel.contains(model))

  def getModelById(id: Id): Option[Model] = models.find(_.name == id)
  def getModelById_!(id: Id): Model       = getModelById(id).getOrElse(throw SharedErrors.InvalidModel(id))

  def getModelByStableIdentifier_!(stableId: String): Model = {
    models.find(_.stableIdentifier == stableId).getOrElse(throw SharedErrors.InvalidModel(s"Could not find a model for the stable identifier: $stableId"))
  }

  // note: mysql columns are case insensitive, so we have to be as well. But we could make them case sensitive https://dev.mysql.com/doc/refman/5.6/en/case-sensitivity.html
  def getModelByName(name: String): Option[Model] = models.find(_.name.toLowerCase() == name.toLowerCase())
  def getModelByName_!(name: String): Model       = getModelByName(name).getOrElse(throw SharedErrors.InvalidModel(s"No model with name: $name found."))

  def getFieldByName(model: String, name: String): Option[Field] = getModelByName(model).flatMap(_.getFieldByName(name))
  def getFieldByName_!(model: String, name: String): Field       = getModelByName_!(model).getFieldByName_!(name)

  def getFieldConstraintById(id: Id): Option[FieldConstraint] = {
    val fields      = models.flatMap(_.fields)
    val constraints = fields.flatMap(_.constraints)
    constraints.find(_.id == id)
  }
  def getFieldConstraintById_!(id: Id): FieldConstraint = getFieldConstraintById(id).get //OrElse(throw SystemErrors.InvalidFieldConstraintId(id))

  // note: mysql columns are case insensitive, so we have to be as well
  def getEnumByName(name: String): Option[Enum] = enums.find(_.name.toLowerCase == name.toLowerCase)

  def getRelationByName(name: String): Option[Relation] = relations.find(_.name == name)
  def getRelationByName_!(name: String): Relation       = getRelationByName(name).get

  def getRelationsThatConnectModels(modelA: String, modelB: String): List[Relation] = relations.filter(_.connectsTheModels(modelA, modelB))

}
