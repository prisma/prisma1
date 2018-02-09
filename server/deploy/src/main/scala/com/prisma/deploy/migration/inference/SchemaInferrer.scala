package com.prisma.deploy.migration.inference

import com.prisma.deploy.gc_value.GCStringConverter
import com.prisma.deploy.migration.DataSchemaAstExtensions._
import com.prisma.deploy.migration.ReservedFields
import com.prisma.deploy.schema.InvalidRelationName
import com.prisma.deploy.validation.NameConstraints
import com.prisma.gc_values.{GCValue, InvalidValueForScalarType}
import com.prisma.shared.models.{OnDelete, _}
import com.prisma.utils.or.OrExtensions
import cool.graph.cuid.Cuid
import org.scalactic.{Bad, Good, Or}
import sangria.ast.{Field => _, _}

trait SchemaInferrer {
  def infer(baseSchema: Schema, schemaMapping: SchemaMapping, graphQlSdl: Document): Schema Or ProjectSyntaxError
}

object SchemaInferrer {
  def apply() = new SchemaInferrer {
    override def infer(baseSchema: Schema, schemaMapping: SchemaMapping, graphQlSdl: Document) =
      SchemaInferrerImpl(baseSchema, schemaMapping, graphQlSdl).infer()
  }
}

sealed trait ProjectSyntaxError
case class RelationDirectiveNeeded(type1: String, type1Fields: Vector[String], type2: String, type2Fields: Vector[String]) extends ProjectSyntaxError
case class InvalidGCValue(err: InvalidValueForScalarType)                                                                  extends ProjectSyntaxError

case class SchemaInferrerImpl(
    baseSchema: Schema,
    schemaMapping: SchemaMapping,
    sdl: Document
) {
  def infer(): Schema Or ProjectSyntaxError = {
    for {
      models <- nextModels
    } yield {
      Schema(
        models = models.toList,
        relations = nextRelations.toList,
        enums = nextEnums.toList
      )
    }
  }

  lazy val nextModels: Vector[Model] Or ProjectSyntaxError = {
    val models = sdl.objectTypes.map { objectType =>
      fieldsForType(objectType).map { fields =>
        val fieldNames            = fields.map(_.name)
        val missingReservedFields = ReservedFields.reservedFieldNames.filterNot(fieldNames.contains)
        val hiddenReservedFields  = missingReservedFields.map(ReservedFields.reservedFieldFor(_).copy(isHidden = true))

        val stableIdentifier = baseSchema.getModelByName(schemaMapping.getPreviousModelName(objectType.name)) match {
          case Some(existingModel) => existingModel.stableIdentifier
          case None                => Cuid.createCuid()
        }

        Model(
          name = objectType.name,
          fields = fields.toList ++ hiddenReservedFields,
          stableIdentifier = stableIdentifier
        )
      }
    }

    OrExtensions.sequence(models)
  }

  def fieldsForType(objectType: ObjectTypeDefinition): Or[Vector[Field], InvalidGCValue] = {
    val fields: Vector[Or[Field, InvalidGCValue]] = objectType.fields.flatMap { fieldDef =>
      val typeIdentifier = typeIdentifierForTypename(fieldDef.typeName)

      val relation = if (fieldDef.hasScalarType) {
        None
      } else {
        fieldDef.relationName match {
          case Some(name) => nextRelations.find(_.name == name)
          case None       => nextRelations.find(relation => relation.connectsTheModels(objectType.name, fieldDef.typeName))
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
          relationSide = relation.map { relation =>
            if (relation.modelAId == objectType.name) {
              RelationSide.A
            } else {
              RelationSide.B
            }
          }
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
      relationField <- objectType.fields if typeIdentifierForTypename(relationField.typeName) == TypeIdentifier.Relation
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

      val relationNameOnRelatedField: Option[String] = sdl.relatedFieldOf(objectType, relationField).flatMap(_.relationName)
      val relationName = (relationField.relationName, relationNameOnRelatedField) match {
        case (Some(name), _) if !NameConstraints.isValidRelationName(name)    => throw InvalidRelationName(name)
        case (None, Some(name)) if !NameConstraints.isValidRelationName(name) => throw InvalidRelationName(name)
        case (Some(name), _)                                                  => name
        case (None, Some(name))                                               => name
        case (None, None)                                                     => generateRelationName
      }
      val previousModelAName = schemaMapping.getPreviousModelName(modelA)
      val previousModelBName = schemaMapping.getPreviousModelName(modelB)

      // TODO: this needs to be adapted once we allow rename of relations
      val oldEquivalentRelation = relationField.relationName.flatMap(baseSchema.getRelationByName).orElse {
        baseSchema.getUnambiguousRelationThatConnectsModels_!(previousModelAName, previousModelBName)
      }

      oldEquivalentRelation match {
        case Some(relation) =>
          val nextModelAId = if (previousModelAName == relation.modelAId) modelA else modelB
          val nextModelBId = if (previousModelBName == relation.modelBId) modelB else modelA
          relation.copy(
            name = relationName,
            modelAId = nextModelAId,
            modelBId = nextModelBId,
            modelAOnDelete = modelAOnDelete,
            modelBOnDelete = modelBOnDelete
          )
        case None =>
          Relation(
            name = relationName,
            modelAId = modelA,
            modelBId = modelB,
            modelAOnDelete = modelAOnDelete,
            modelBOnDelete = modelBOnDelete
          )
      }
    }
    tmp.groupBy(_.name).values.flatMap(_.headOption).toSet
  }

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
}
