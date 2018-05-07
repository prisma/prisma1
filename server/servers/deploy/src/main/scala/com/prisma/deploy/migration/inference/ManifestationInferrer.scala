//package com.prisma.deploy.migration.inference
//
//import com.prisma.deploy.connector.{DatabaseIntrospectionInferrer, InferredTables}
//import com.prisma.shared.models.Manifestations._
//import com.prisma.shared.models._
//import org.scalactic.Or
//import sangria.ast.{Document, FieldDefinition, ObjectTypeDefinition}
//
//trait ManifestationError
//
//trait ManifestationInferrer {
//  def infer(schema: Schema, databaseIntrospectionInferrer: DatabaseIntrospectionInferrer): Schema Or ManifestationError
//}
//
//object ManifestationInferrer {
//  def apply(schema: Schema, databaseIntrospectionInferrer: DatabaseIntrospectionInferrer): ManifestationInferrer = new ManifestationInferrer {
//    override def infer(schema: Schema, databaseIntrospectionInferrer: DatabaseIntrospectionInferrer) = {
//      ManifestationInferrerImpl(schema, databaseIntrospectionInferrer).infer()
//    }
//  }
//}
//
//case class ManifestationInferrerImpl(
//    sdl: Document,
//    schema: Schema,
//    inferredTables: InferredTables
//) {
//  import com.prisma.deploy.migration.DataSchemaAstExtensions._
//
//  def infer(): Schema Or ManifestationError = {
//    ???
//  }
//
//  lazy val transformedRelations: List[Relation] = schema.relations.map { relation =>
//    val modelA
//    ???
//  }
//
//  lazy val transformedModels: List[Model] = schema.models.map { model =>
//    val objectType    = sdl.objectType_!(model.name)
//    val manifestation = objectType.tableNameDirective.map(ModelManifestation)
//    model.copy(
//      manifestation = manifestation,
//      fields = transformModelFields(model)
//    )
//  }
//
//  def transformModelFields(model: Model): List[Field] = model.fields.map { field =>
//    val objectType    = sdl.objectType_!(model.name)
//    val fieldDef      = objectType.field_!(field.name)
//    val manifestation = fieldDef.columnName.map(FieldManifestation)
//    field.copy(manifestation = manifestation)
//  }
//
//  def relationManifestationOnFieldOrRelatedField(objectType: ObjectTypeDefinition, relationField: FieldDefinition): Option[RelationManifestation] = {
//    val manifestationOnThisField = relationManifestationOnField(objectType, relationField)
//    val manifestationOnRelatedField = sdl.relatedFieldOf(objectType, relationField).flatMap { relatedField =>
//      val relatedType = sdl.objectType_!(relationField.typeName)
//      relationManifestationOnField(relatedType, relatedField)
//    }
//    manifestationOnThisField.orElse(manifestationOnRelatedField)
//  }
//
//  def relationManifestationOnField(objectType: ObjectTypeDefinition, relationField: FieldDefinition): Option[RelationManifestation] = {
//    require(isRelationField(relationField), "this method must only be called with relationFields")
//    val inlineRelationManifestation = relationField.inlineRelationDirective.column
//      .orElse {
//        // FIXME: the table name for the model may diverge
//        inferredTables.modelTables.find(_.name == objectType.tableName).get.columnNameForReferencedTable(relationField.typeName)
//      }
//      .map { column =>
//        InlineRelationManifestation(inTableOfModelId = objectType.name, referencingColumn = column)
//      }
//
//    val relationTableManifestation = relationField.relationTableDirective.map { tableDirective =>
//      val isThisModelA  = isModelA(objectType.name, relationField.typeName)
//      val inferredTable = inferredTables.relationTables.find(_.name == tableDirective.table)
//
//      val modelAColumn = if (isThisModelA) {
//        tableDirective.thisColumn.orElse(inferredTable.flatMap(_.columnForTable(relationField.typeName)))
//      } else {
//        tableDirective.otherColumn.orElse(inferredTable.flatMap(_.columnForTable(objectType.name)))
//      }
//
//      val modelBColumn = if (isThisModelA) {
//        tableDirective.otherColumn.orElse(inferredTable.flatMap(_.columnForTable(objectType.name)))
//      } else {
//        tableDirective.thisColumn.orElse(inferredTable.flatMap(_.columnForTable(relationField.typeName)))
//      }
//
//      // FIXME: return an error if we can not find a foreign key columns for his relation instead of calling .get
//      RelationTableManifestation(
//        table = tableDirective.table,
//        modelAColumn = modelAColumn.get,
//        modelBColumn = modelBColumn.get
//      )
//    }
//    inlineRelationManifestation.orElse(relationTableManifestation)
//  }
//
//  def isModelA(model1: String, model2: String): Boolean = model1 < model2
//
//  def isRelationField(fieldDef: FieldDefinition): Boolean = typeIdentifierForTypename(fieldDef.typeName) == TypeIdentifier.Relation
//}
