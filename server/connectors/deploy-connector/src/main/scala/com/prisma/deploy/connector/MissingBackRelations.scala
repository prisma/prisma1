package com.prisma.deploy.connector

import com.prisma.shared.models._

// this object should be called by all implementations of ProjectPersistence to ensure that back relation fields are always available
object MissingBackRelations {

  def add(schema: Schema): Schema = schema.relations.foldLeft(schema) { (schema, relation) =>
    addMissingBackRelationFieldIfMissing(schema, relation)
  }

  private def addMissingBackRelationFieldIfMissing(schema: Schema, relation: Relation): Schema = {
    val isAFieldMissing = !relation.modelA.relationFields.exists(_.relation == relation)
    val isBFieldMissing = !relation.modelB.relationFields.exists(_.relation == relation)
    if (isAFieldMissing) {
      addMissingFieldFor(schema, relation, RelationSide.A)
    } else if (isBFieldMissing) {
      addMissingFieldFor(schema, relation, RelationSide.B)
    } else {
      schema
    }
  }

  private def addMissingFieldFor(schema: Schema, relation: Relation, relationSide: RelationSide.Value): Schema = {
    val model     = if (relationSide == RelationSide.A) relation.modelA else relation.modelB
    val newModel  = model.copy(fieldTemplates = model.fieldTemplates :+ missingBackRelationField(relation, relationSide))
    val newModels = schema.modelTemplates.filter(_.name != model.name) :+ newModel
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
      isReadonly = false,
      enum = None,
      defaultValue = None,
      relationName = Some(relation.name),
      relationSide = Some(relationSide),
      manifestation = None
    )
  }
}
