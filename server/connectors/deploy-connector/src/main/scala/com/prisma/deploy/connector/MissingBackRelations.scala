package com.prisma.deploy.connector

import com.prisma.shared.models._

// this object should be called by all implementations of ProjectPersistence to ensure that back relation fields are always available
object MissingBackRelations {

  def add(schema: Schema): Schema = schema.relations.foldLeft(schema) { (schema, relation) =>
    addMissingBackRelationFieldIfMissing(schema, relation)
  }

  private def addMissingBackRelationFieldIfMissing(schema: Schema, relation: Relation): Schema = {
    val isAFieldMissing = !relation.modelA.relationFields.exists(field => field.relation == relation && field.relationSide == RelationSide.A)
    val isBFieldMissing = !relation.modelB.relationFields.exists(field => field.relation == relation && field.relationSide == RelationSide.B)
    if (isAFieldMissing) {
      addMissingFieldFor(schema, relation, RelationSide.A)
    } else if (isBFieldMissing) {
      addMissingFieldFor(schema, relation, RelationSide.B)
    } else {
      schema
    }
  }

  private def addMissingFieldFor(schema: Schema, relation: Relation, relationSide: RelationSide.Value): Schema = {
    val relationFromUpdatedSchema = schema.relations.find(_.name == relation.name).get
    val model                     = if (relationSide == RelationSide.A) relationFromUpdatedSchema.modelA else relationFromUpdatedSchema.modelB
    val newModel                  = model.copy(fieldTemplates = model.fieldTemplates :+ missingBackRelationField(relationFromUpdatedSchema, relationSide))
    val newModels                 = schema.modelTemplates.filter(_.name != model.name) :+ newModel
    schema.copy(modelTemplates = newModels)
  }

  private def missingBackRelationField(relation: Relation, relationSide: RelationSide.Value): FieldTemplate = {
    val name = Field.magicalBackRelationPrefix + relation.name
    FieldTemplate(
      name = name,
      typeIdentifier = TypeIdentifier.Relation,
      isRequired = false,
      isList = true,
      isUnique = false,
      isHidden = true,
      enum = None,
      defaultValue = None,
      relationName = Some(relation.name),
      relationSide = Some(relationSide),
      manifestation = None,
      behaviour = None
    )
  }
}
