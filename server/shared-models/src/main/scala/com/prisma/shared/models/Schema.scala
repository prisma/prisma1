package com.prisma.shared.models

import com.prisma.shared.errors.SharedErrors

object Schema {
  val empty    = Schema()
  val emptyV11 = Schema(version = Some(version.v11))

  object version {
    val v11 = "v2" // the datamodel v1.1 was originally called v2. For backwards compatibility we still call it v2 here.
  }
}

case class Schema(
    modelTemplates: List[ModelTemplate] = List.empty,
    relationTemplates: List[RelationTemplate] = List.empty,
    enums: List[Enum] = List.empty,
    version: Option[String] = None
) {
  val isLegacy                              = version.isEmpty
  val isV11                                 = version.contains(Schema.version.v11)
  val models                                = modelTemplates.map(_.build(this))
  val relations                             = relationTemplates.map(_.build(this))
  val allFields: Seq[Field]                 = models.flatMap(_.fields)
  val allRelationFields: Seq[RelationField] = models.flatMap(_.relationFields)

  def fieldsThatLinkToThisModel(model: Model)      = allRelationFields.filter(_.relatedModel_! == model)
  def fieldsWhereThisModelIsRequired(model: Model) = fieldsThatLinkToThisModel(model).filter(f => f.isRequired && !f.isList)

  def getModelByStableIdentifier_!(stableId: String): Model = {
    models.find(_.stableIdentifier == stableId).getOrElse(throw SharedErrors.InvalidModel(s"Could not find a model for the stable identifier: $stableId"))
  }

  def getModelByName_!(name: String): Model                                         = getModelByName(name).getOrElse(throw SharedErrors.InvalidModel(s"No model with name: $name found."))
  def getModelByName(name: String): Option[Model]                                   = models.find(_.name == name)
  def getFieldByName_!(model: String, name: String): Field                          = getModelByName_!(model).getFieldByName_!(name)
  def getFieldByName(model: String, name: String): Option[Field]                    = getModelByName(model).flatMap(_.getFieldByName(name))
  def getEnumByName_!(name: String): Enum                                           = getEnumByName(name).get
  def getEnumByName(name: String): Option[Enum]                                     = enums.find(_.name == name)
  def getRelationByName_!(name: String): Relation                                   = getRelationByName(name).get
  def getRelationByName(name: String): Option[Relation]                             = relations.find(_.name == name)
  def getRelationsThatConnectModels(modelA: String, modelB: String): List[Relation] = relations.filter(_.connectsTheModels(modelA, modelB))
}
