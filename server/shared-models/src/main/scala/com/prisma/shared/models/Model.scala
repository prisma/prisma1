package com.prisma.shared.models

import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.Manifestations.ModelManifestation
import scala.language.implicitConversions

case class ModelTemplate(
    name: String,
    stableIdentifier: String,
    fieldTemplates: List[FieldTemplate],
    manifestation: Option[ModelManifestation]
) {
  def build(schema: Schema): Model = new Model(this, schema)
}

object Model {
  implicit def asModelTemplate(model: Model): ModelTemplate = model.template

  val empty: Model = new Model(
    template = ModelTemplate(name = "", stableIdentifier = "", fieldTemplates = List.empty, manifestation = None),
    schema = Schema.empty
  )
}
class Model(
    val template: ModelTemplate,
    val schema: Schema
) {
  import template._

  val dbName: String                            = manifestation.map(_.dbName).getOrElse(name)
  lazy val fields: List[Field]                  = fieldTemplates.map(_.build(this))
  lazy val uniqueFields: List[Field]            = fields.filter(f => f.isUnique && f.isVisible)
  lazy val scalarFields: List[Field]            = fields.filter(_.isScalar)
  lazy val scalarListFields: List[Field]        = scalarFields.filter(_.isList)
  lazy val scalarNonListFields: List[Field]     = scalarFields.filter(!_.isList)
  lazy val relationFields: List[Field]          = fields.filter(_.isRelation)
  lazy val relationListFields: List[Field]      = relationFields.filter(_.isList)
  lazy val relationNonListFields: List[Field]   = relationFields.filter(!_.isList)
  lazy val visibleRelationFields: List[Field]   = relationFields.filter(_.isVisible)
  lazy val relations: List[Relation]            = fields.flatMap(_.relation).distinct
  lazy val nonListFields                        = fields.filter(!_.isList)
  lazy val idField                              = getFieldByName("id")
  lazy val idField_!                            = getFieldByName_!("id")
  lazy val dbNameOfIdField_!                    = idField_!.dbName
  val updatedAtField                            = getFieldByName("updatedAt")
  lazy val cascadingRelationFields: List[Field] = relationFields.filter(field => field.relation.get.sideOfModelCascades(this))

  def relationFieldForIdAndSide(relationId: String, relationSide: RelationSide.Value): Option[Field] = {
    fields.find(_.isRelationWithIdAndSide(relationId, relationSide))
  }

  def filterFields(fn: Field => Boolean): Model = {
    val newFields         = this.fields.filter(fn).map(_.template)
    val newModel          = copy(fieldTemplates = newFields)
    val newModelsInSchema = schema.models.filter(_.name != name).map(_.template) :+ newModel
    schema.copy(modelTemplates = newModelsInSchema).getModelByName_!(name)
  }

  def getFieldByName_!(name: String): Field       = getFieldByName(name).getOrElse(sys.error(s"field $name is not part of the model ${this.name}"))
  def getFieldByName(name: String): Option[Field] = fields.find(_.name == name)

  def hasVisibleIdField: Boolean = idField.exists(_.isVisible)
}
