package cool.graph.deploy.migration

import cool.graph.deploy.gc_value.GCStringConverter
import cool.graph.gc_values.{GCValue, InvalidValueForScalarType}
import cool.graph.shared.models._
import cool.graph.utils.or.OrExtensions
import org.scalactic.{Bad, Good, Or}
import sangria.ast.{Document, ObjectTypeDefinition}

trait NextProjectInferer {
  def infer(baseProject: Project, renames: Renames, graphQlSdl: Document): Project Or ProjectSyntaxError
}

sealed trait ProjectSyntaxError
case class RelationDirectiveNeeded(type1: String, type1Fields: Vector[String], type2: String, type2Fields: Vector[String]) extends ProjectSyntaxError
case class InvalidGCValue(err: InvalidValueForScalarType)                                                                  extends ProjectSyntaxError

object NextProjectInferer {
  def apply() = new NextProjectInferer {
    override def infer(baseProject: Project, renames: Renames, graphQlSdl: Document) = NextProjectInfererImpl(baseProject, renames, graphQlSdl).infer()
  }
}

case class NextProjectInfererImpl(
    baseProject: Project,
    renames: Renames,
    sdl: Document
) {
  import DataSchemaAstExtensions._

  def infer(): Project Or ProjectSyntaxError = {
    for {
      models <- nextModels
    } yield {
      val newProject = Project(
        id = baseProject.id,
        ownerId = baseProject.ownerId,
        models = models.toList,
        relations = nextRelations.toList,
        enums = nextEnums.toList
      )

      newProject
    }
  }

  lazy val nextModels: Vector[Model] Or ProjectSyntaxError = {
    val models = sdl.objectTypes.map { objectType =>
      fieldsForType(objectType) match {
        case Good(fields: Vector[Field]) =>
          val fieldNames            = fields.map(_.name)
          val missingReservedFields = ReservedFields.reservedFieldNames.filterNot(fieldNames.contains)
          val hiddenReservedFields  = missingReservedFields.map(ReservedFields.reservedFieldFor(_).copy(isHidden = true))

          Good {
            Model(
              name = objectType.name,
              fields = fields.toList ++ hiddenReservedFields
            )
          }

        case Bad(err) =>
          Bad(err)
      }
    }

    OrExtensions.sequence(models)
  }

  def fieldsForType(objectType: ObjectTypeDefinition): Or[Vector[Field], InvalidGCValue] = {
    val fields: Seq[Or[Field, InvalidGCValue]] = objectType.fields.flatMap { fieldDef =>
      val typeIdentifier = typeIdentifierForTypename(fieldDef.typeName)

      val relation = if (fieldDef.hasScalarType) {
        None
      } else {
        nextRelations.find { relation =>
          relation.connectsTheModels(objectType.name, fieldDef.typeName)
        }
      }

      def fieldWithDefault(default: Option[GCValue]) = {
        Field(
          id = fieldDef.name,
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

    OrExtensions.sequence(fields.toVector)
  }

  lazy val nextRelations: Set[Relation] = {
    val tmp = for {
      objectType    <- sdl.objectTypes
      relationField <- objectType.fields if typeIdentifierForTypename(relationField.typeName) == TypeIdentifier.Relation
    } yield {
      val model1           = objectType.name
      val model2           = relationField.typeName
      val (modelA, modelB) = if (model1 < model2) (model1, model2) else (model2, model1)

      /**
        * 1: has relation directive. use that one.
        * 2: has no relation directive but there's a related field with directive. Use name of the related field.
        * 3: use auto generated name else
        */
      val relationNameOnRelatedField: Option[String] = sdl.relatedFieldOf(objectType, relationField).flatMap(_.relationName)
      val relationName = (relationField.relationName, relationNameOnRelatedField) match {
        case (Some(name), _)    => name
        case (None, Some(name)) => name
        case (None, None)       => s"${modelA}To${modelB}"
      }
      val previousModelAName = renames.getPreviousModelName(modelA)
      val previousModelBName = renames.getPreviousModelName(modelB)

      // TODO: this needs to be adapted once we allow rename of relations
      val oldEquivalentRelation = relationField.relationName.flatMap(baseProject.getRelationByName).orElse {
        baseProject.getUnambiguousRelationThatConnectsModels_!(previousModelAName, previousModelBName)
      }

      oldEquivalentRelation match {
        case Some(relation) =>
          val nextModelAId = if (previousModelAName == relation.modelAId) modelA else modelB
          val nextModelBId = if (previousModelBName == relation.modelBId) modelB else modelA
          relation.copy(
            name = relationName,
            modelAId = nextModelAId,
            modelBId = nextModelBId
          )
        case None =>
          Relation(
            id = relationName,
            name = relationName,
            modelAId = modelA,
            modelBId = modelB
          )
      }
    }

    tmp.groupBy(_.name).values.flatMap(_.headOption).toSet
  }

  lazy val nextEnums: Vector[Enum] = {
    sdl.enumTypes.map { enumDef =>
      Enum(
        name = enumDef.name,
        values = enumDef.values.map(_.name)
      )
    }
  }

  private def typeIdentifierForTypename(typeName: String): TypeIdentifier.Value = {
    if (sdl.objectType(typeName).isDefined) {
      TypeIdentifier.Relation
    } else if (sdl.enumType(typeName).isDefined) {
      TypeIdentifier.Enum
    } else {
      TypeIdentifier.withNameHacked(typeName)
    }
  }
}
