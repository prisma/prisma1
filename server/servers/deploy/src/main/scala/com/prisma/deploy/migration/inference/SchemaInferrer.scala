package com.prisma.deploy.migration.inference

import com.prisma.deploy.connector.{DatabaseIntrospectionInferrer, EmptyDatabaseIntrospectionInferrer, InferredRelationTable, InferredTables}
import com.prisma.deploy.gc_value.GCStringConverter
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.DirectiveTypes.RelationTableDirective
import com.prisma.deploy.schema.InvalidRelationName
import com.prisma.deploy.validation.NameConstraints
import com.prisma.gc_values.{GCValue, InvalidValueForScalarType}
import com.prisma.shared.models.Manifestations._
import com.prisma.shared.models.{OnDelete, RelationSide, ReservedFields, _}
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.or.OrExtensions
import cool.graph.cuid.Cuid
import org.scalactic.{Bad, Good, Or}
import sangria.ast.{Field => _, _}

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

trait SchemaInferrer {
  def infer(baseSchema: Schema, schemaMapping: SchemaMapping, graphQlSdl: Document, inferredTables: InferredTables): Schema Or ProjectSyntaxError
}

object SchemaInferrer {
  def apply(isActive: Boolean = true) = new SchemaInferrer {
    override def infer(baseSchema: Schema, schemaMapping: SchemaMapping, graphQlSdl: Document, inferredTables: InferredTables) =
      SchemaInferrerImpl(baseSchema, schemaMapping, graphQlSdl, isActive, inferredTables).infer()
  }
}

sealed trait ProjectSyntaxError                                                                                            extends Exception
case class RelationDirectiveNeeded(type1: String, type1Fields: Vector[String], type2: String, type2Fields: Vector[String]) extends ProjectSyntaxError
case class InvalidGCValue(err: InvalidValueForScalarType)                                                                  extends ProjectSyntaxError
case class GenericProblem(msg: String)                                                                                     extends ProjectSyntaxError

case class ProjectSyntaxErrorException(error: ProjectSyntaxError) extends Exception

case class SchemaInferrerImpl(
    baseSchema: Schema,
    schemaMapping: SchemaMapping,
    sdl: Document,
    isActive: Boolean,
    inferredTables: InferredTables
) extends AwaitUtils {

  def infer(): Schema Or ProjectSyntaxError = {
    for {
      models <- nextModels(nextRelations)
      schema = Schema(
        models = models.toList,
        relations = nextRelations.toList,
        enums = nextEnums.toList
      )
      errors = if (!isActive) checkRelationsAgainstInferredTables(schema) else Vector.empty
      result <- if (errors.isEmpty) Good(schema) else Bad(errors.head)
    } yield result
  }

  def nextModels(relations: Set[Relation]): Vector[Model] Or ProjectSyntaxError = {
    val models = sdl.objectTypes.map { objectType =>
      fieldsForType(objectType, relations).map { fields =>
        val fieldNames = fields.map(_.name)
        val hiddenReservedFields = if (isActive) {
          val missingReservedFields = ReservedFields.reservedFieldNames.filterNot(fieldNames.contains)
          missingReservedFields.map(ReservedFields.reservedFieldFor(_).copy(isHidden = true))
        } else {
          Vector.empty
        }
        val manifestation = objectType.tableNameDirective.map(ModelManifestation)

        val stableIdentifier = baseSchema.getModelByName(schemaMapping.getPreviousModelName(objectType.name)) match {
          case Some(existingModel) => existingModel.stableIdentifier
          case None                => Cuid.createCuid()
        }

        Model(
          name = objectType.name,
          fields = fields.toList ++ hiddenReservedFields,
          stableIdentifier = stableIdentifier,
          manifestation = manifestation
        )
      }
    }

    OrExtensions.sequence(models)
  }

  def fieldsForType(objectType: ObjectTypeDefinition, relations: Set[Relation]): Or[Vector[Field], InvalidGCValue] = {

    val fields: Vector[Or[Field, InvalidGCValue]] = objectType.fields.flatMap { fieldDef =>
      val typeIdentifier = typeIdentifierForTypename(fieldDef.typeName)

      val relation = if (fieldDef.hasScalarType) {
        None
      } else {
        fieldDef.relationName match {
          case Some(name) => relations.find(_.name == name)
          case None       => relations.find(relation => relation.connectsTheModels(objectType.name, fieldDef.typeName))
        }
      }

      //For self relations we were inferring the relationSide A for both sides, this now assigns A to the lexicographically lower field name and B to the other
      //If in the previous schema whether both relationSides are A we reassign the relationsides otherwise we keep the one from the previous schema.
      def inferRelationSide(relation: Option[Relation]): Option[RelationSide.Value] = {
        def oldRelationSidesNotBothEqual(oldField: Field) = oldField.otherRelationField(baseSchema) match {
          case Some(relatedField) => oldField.relationSide.isDefined && oldField.relationSide != relatedField.relationSide
          case None               => true
        }

        relation.map { relation =>
          if (relation.isSameModelRelation) {
            val oldFieldName = schemaMapping.getPreviousFieldName(objectType.name, fieldDef.name)
            val oldModelName = schemaMapping.getPreviousModelName(objectType.name)
            val oldField     = baseSchema.getFieldByName(oldModelName, oldFieldName)

            oldField match {
              case Some(field) if field.isRelation && oldRelationSidesNotBothEqual(field) =>
                field.relationSide.get

              case _ =>
                val relationFieldNames = objectType.fields.filter(f => f.relationName.contains(relation.name)).map(_.name)
                if (relationFieldNames.exists(name => name < fieldDef.name)) RelationSide.B else RelationSide.A
            }
          } else {
            if (relation.modelAId == objectType.name) RelationSide.A else RelationSide.B
          }
        }
      }

      def fieldWithDefault(default: Option[GCValue]) = {
        Field(
          name = fieldDef.name,
          typeIdentifier = typeIdentifier,
          isRequired = fieldDef.isRequired,
          isList = fieldDef.isList,
          isUnique = fieldDef.isUnique,
          enum = nextEnums.find(_.name == fieldDef.typeName),
          defaultValue = default,
          relation = relation,
          relationSide = inferRelationSide(relation),
          manifestation = fieldDef.columnName.map(FieldManifestation)
        )
      }

      fieldDef.defaultValue.map(x => GCStringConverter(typeIdentifier, fieldDef.isList).toGCValue(x)) match {
        case Some(Good(gcValue)) => Some(Good(fieldWithDefault(Some(gcValue))))
        case Some(Bad(err))      => Some(Bad(InvalidGCValue(err)))
        case None                => Some(Good(fieldWithDefault(None)))
      }
    }

    OrExtensions.sequence(fields)
  }

  lazy val nextRelations: Set[Relation] = {
    val tmp = for {
      objectType    <- sdl.objectTypes
      relationField <- objectType.fields if isRelationField(relationField)
    } yield {
      val model1 = objectType.name
      val model2 = relationField.typeName

      val model1OnDelete: OnDelete.Value = getOnDeleteFromField(relationField)
      val model2OnDelete: OnDelete.Value = sdl.relatedFieldOf(objectType, relationField).map(getOnDeleteFromField).getOrElse(OnDelete.SetNull)

      val (modelA, modelAOnDelete, modelB, modelBOnDelete) =
        if (model1 < model2) (model1, model1OnDelete, model2, model2OnDelete) else (model2, model2OnDelete, model1, model1OnDelete)

      /**
        * 1: has relation directive. use that one.
        * 2: has no relation directive but there's a related field with directive. Use name of the related field.
        * 3: use auto generated name else
        */
      def generateRelationName: String = {
        def concat(modelName: String, otherModelName: String): String = {
          val concatenatedString = s"${modelName}To${otherModelName}"

          !NameConstraints.isValidRelationName(concatenatedString) match {
            case true if otherModelName.length > modelName.length => concat(modelName, otherModelName.substring(0, otherModelName.length - 1))
            case true                                             => concat(modelName.substring(0, modelName.length - 1), otherModelName)
            case false                                            => concatenatedString
          }
        }
        concat(modelA, modelB)
      }

      val relatedField                               = sdl.relatedFieldOf(objectType, relationField)
      val relationNameOnRelatedField: Option[String] = relatedField.flatMap(_.relationName)
      val relationName = (relationField.relationName, relationNameOnRelatedField) match {
        case (Some(name), _) if !NameConstraints.isValidRelationName(name)    => throw InvalidRelationName(name)
        case (None, Some(name)) if !NameConstraints.isValidRelationName(name) => throw InvalidRelationName(name)
        case (Some(name), _)                                                  => name
        case (None, Some(name))                                               => name
        case (None, None)                                                     => generateRelationName
      }
      val previousModelAName = schemaMapping.getPreviousModelName(modelA)
      val previousModelBName = schemaMapping.getPreviousModelName(modelB)

      val oldEquivalentRelation = relationField.relationName.flatMap(baseSchema.getRelationByName).orElse {
        UnambiguousRelation.unambiguousRelationThatConnectsModels(baseSchema, previousModelAName, previousModelBName)
      }
      val relationManifestation = relationManifestationOnFieldOrRelatedField(objectType, relationField)

      oldEquivalentRelation match {
        case Some(relation) =>
          val nextModelAId = if (previousModelAName == relation.modelAId) modelA else modelB
          val nextModelBId = if (previousModelBName == relation.modelBId) modelB else modelA
          relation.copy(
            name = relationName,
            modelAId = nextModelAId,
            modelBId = nextModelBId,
            modelAOnDelete = modelAOnDelete,
            modelBOnDelete = modelBOnDelete,
            manifestation = relationManifestation
          )
        case None =>
          Relation(
            name = relationName,
            modelAId = modelA,
            modelBId = modelB,
            modelAOnDelete = modelAOnDelete,
            modelBOnDelete = modelBOnDelete,
            manifestation = relationManifestation
          )
      }
    }
    tmp.groupBy(_.name).values.flatMap(_.headOption).toSet
  }

  def checkRelationsAgainstInferredTables(schema: Schema): immutable.Seq[GenericProblem] = {
    schema.relations.flatMap { relation =>
      relation.manifestation match {
        case None =>
          val modelA = relation.getModelA_!(schema)
          val modelB = relation.getModelB_!(schema)
          Some(GenericProblem(s"Could not find the relation between the models ${modelA.name} and ${modelB.name} in the databtase"))

        case Some(m: InlineRelationManifestation) =>
          val model = schema.getModelById_!(m.inTableOfModelId)
          val field = relation.getFieldOnModel(model.id, schema).get
          inferredTables.modelTables.find(_.name == model.dbName) match {
            case None =>
              Some(GenericProblem(s"Could not find the model table ${model.dbName} in the databse"))

            case Some(modelTable) =>
              modelTable.foreignKeys.find(_.name == m.referencingColumn) match {
                case None    => Some(GenericProblem(s"Could not find the foreign key column ${m.referencingColumn} in the model table ${model.dbName}"))
                case Some(_) => None
              }
          }

        case Some(m: RelationTableManifestation) =>
          inferredTables.relationTables.find(_.name == m.table) match {
            case None =>
              Some(GenericProblem(s"Could not find the relation table ${m.table}"))

            case Some(relationTable) =>
              val modelA = relation.getModelA_!(schema)
              val modelB = relation.getModelB_!(schema)
              if (!relationTable.referencesTheTables(modelA.dbName, modelB.dbName)) {
                Some(GenericProblem(s"The specified relation table ${m.table} does not reference the tables for model ${modelA.name} and ${modelB.name}"))
              } else if (!relationTable.doesColumnReferenceTable(m.modelAColumn, modelA.dbName)) {
                Some(GenericProblem(
                  s"The specified relation table ${m.table} does not have a column ${m.modelAColumn} or does not the reference the right table ${modelA.dbName}"))
              } else if (!relationTable.doesColumnReferenceTable(m.modelBColumn, modelB.dbName)) {
                Some(GenericProblem(
                  s"The specified relation table ${m.table} does not have a column ${m.modelBColumn} or does not the reference the right table ${modelB.dbName}"))
              } else {
                None
              }
          }
      }
    }
  }

  def relationManifestationOnFieldOrRelatedField(objectType: ObjectTypeDefinition, relationField: FieldDefinition): Option[RelationManifestation] = {
    if (isActive) {
      None
    } else {
      val manifestationOnThisField = relationManifestationOnField(objectType, relationField)
      val manifestationOnRelatedField = sdl.relatedFieldOf(objectType, relationField).flatMap { relatedField =>
        val relatedType = sdl.objectType_!(relationField.typeName)
        relationManifestationOnField(relatedType, relatedField)
      }
      manifestationOnThisField.orElse(manifestationOnRelatedField)
    }
  }

  def relationManifestationOnField(objectType: ObjectTypeDefinition, relationField: FieldDefinition): Option[RelationManifestation] = {
    require(isRelationField(relationField), "this method must only be called with relationFields")
    val inlineRelationManifestation = relationField.inlineRelationDirective.column
      .orElse {
        val referencedType = sdl.objectType_!(relationField.typeName)
        inferredTables.modelTable_!(objectType.tableName).columnNameForReferencedTable(referencedType.tableName)
      }
      .map { column =>
        InlineRelationManifestation(inTableOfModelId = objectType.name, referencingColumn = column)
      }

    val relationTableManifestation = relationField.relationTableDirective
      .flatMap { tableDirective =>
        val isThisModelA  = isModelA(objectType.name, relationField.typeName)
        val inferredTable = inferredTables.relationTables.find(_.name == tableDirective.table)
        val relatedType   = sdl.objectType_!(relationField.typeName)

        val modelAColumnOpt = if (isThisModelA) {
          tableDirective.thisColumn.orElse(inferredTable.flatMap(_.columnForTable(relatedType.tableName)))
        } else {
          tableDirective.otherColumn.orElse(inferredTable.flatMap(_.columnForTable(objectType.tableName)))
        }

        val modelBColumnOpt = if (isThisModelA) {
          tableDirective.otherColumn.orElse(inferredTable.flatMap(_.columnForTable(objectType.tableName)))
        } else {
          tableDirective.thisColumn.orElse(inferredTable.flatMap(_.columnForTable(relatedType.tableName)))
        }

        for {
          modelAColumn <- modelAColumnOpt
          modelBColumn <- modelBColumnOpt
        } yield {
          RelationTableManifestation(
            table = tableDirective.table,
            modelAColumn = modelAColumn,
            modelBColumn = modelBColumn
          )
        }
      }
      .orElse {
        val relatedType         = sdl.objectType_!(relationField.typeName)
        val tableForThisType    = objectType.tableName
        val tableForRelatedType = sdl.objectType_!(relationField.typeName).tableName

        inferredTables.relationTables
          .find { relationTable =>
            relationTable.referencesTheTables(tableForThisType, tableForRelatedType)
          }
          .flatMap { inferredTable =>
            val isThisModelA = isModelA(objectType.name, relationField.typeName)
            val modelAColumnOpt = if (isThisModelA) {
              inferredTable.columnForTable(relatedType.tableName)
            } else {
              inferredTable.columnForTable(objectType.tableName)
            }

            val modelBColumnOpt = if (isThisModelA) {
              inferredTable.columnForTable(objectType.tableName)
            } else {
              inferredTable.columnForTable(relatedType.tableName)
            }
            for {
              modelAColumn <- modelAColumnOpt
              modelBColumn <- modelBColumnOpt
            } yield {
              RelationTableManifestation(
                table = inferredTable.name,
                modelAColumn = modelAColumn,
                modelBColumn = modelBColumn
              )
            }
          }
      }
    inlineRelationManifestation.orElse(relationTableManifestation)
  }

  /**
    * returns true if model1 is modelA
    */
  def isModelA(model1: String, model2: String): Boolean = model1 < model2

  def isRelationField(fieldDef: FieldDefinition): Boolean = typeIdentifierForTypename(fieldDef.typeName) == TypeIdentifier.Relation

  private def getOnDeleteFromField(field: FieldDefinition): OnDelete.Value = {
    field.directiveArgumentAsString("relation", "onDelete") match {
      case None             => OnDelete.SetNull
      case Some("SET_NULL") => OnDelete.SetNull
      case Some("CASCADE")  => OnDelete.Cascade
      case Some(_)          => sys.error("Unexpected onDelete enum value. The schema syntax validator should have caught that.")
    }
  }

  lazy val nextEnums: Vector[Enum] = {
    sdl.enumTypes.map { enumDef =>
      Enum(
        name = enumDef.name,
        values = enumDef.values.map(_.name)
      )
    }
  }

  def typeIdentifierForTypename(typeName: String): TypeIdentifier.Value = {
    if (sdl.objectType(typeName).isDefined) {
      TypeIdentifier.Relation
    } else if (sdl.enumType(typeName).isDefined) {
      TypeIdentifier.Enum
    } else {
      TypeIdentifier.withNameHacked(typeName)
    }
  }

  def catchSyntaxExceptions[T](fn: => T): T Or ProjectSyntaxError = {
    Try(fn) match {
      case Success(x)                              => Good(x)
      case Failure(e: ProjectSyntaxErrorException) => Bad(e.error)
      case Failure(e: ProjectSyntaxError)          => Bad(e)
      case Failure(e)                              => throw e
    }
  }
}
