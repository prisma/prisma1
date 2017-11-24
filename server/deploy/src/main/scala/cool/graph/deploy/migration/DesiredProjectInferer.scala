package cool.graph.deploy.migration

import cool.graph.deploy.gc_value.GCStringConverter
import cool.graph.shared.models._
import org.scalactic.{Good, Or}
import sangria.ast.Document

trait DesiredProjectInferer {
  def infer(baseProject: Project, graphQlSdl: Document): Project Or ProjectSyntaxError
}

sealed trait ProjectSyntaxError
case class RelationDirectiveNeeded(type1: String, type1Fields: Vector[String], type2: String, type2Fields: Vector[String]) extends ProjectSyntaxError

object DesiredProjectInferer {
  def apply() = new DesiredProjectInferer {
    override def infer(baseProject: Project, graphQlSdl: Document) = DesiredProjectInfererImpl(baseProject, graphQlSdl).infer()
  }
}

case class DesiredProjectInfererImpl(
    baseProject: Project,
    sdl: Document
) {
  import DataSchemaAstExtensions._

  def infer(): Project Or ProjectSyntaxError = {
    val newProject = Project(
      id = baseProject.id,
      name = baseProject.name,
      alias = baseProject.alias,
      projectDatabase = baseProject.projectDatabase,
      ownerId = baseProject.ownerId,
      models = desiredModels.toList,
      relations = desiredRelations.toList,
      enums = desiredEnums.toList
    )
    Good(newProject)
  }

  lazy val desiredModels: Vector[Model] = {
    sdl.objectTypes.map { objectType =>
      val fields = objectType.fields.map { fieldDef =>
        val typeIdentifier = typeIdentifierForTypename(fieldDef.typeName)
        val relation       = fieldDef.relationName.flatMap(relationName => desiredRelations.find(_.name == relationName))
        Field(
          id = fieldDef.name,
          name = fieldDef.name,
          typeIdentifier = typeIdentifier,
          isRequired = fieldDef.isRequired,
          isList = fieldDef.isList,
          isUnique = fieldDef.isUnique,
          enum = desiredEnums.find(_.name == fieldDef.typeName),
          defaultValue = fieldDef.defaultValue.map(x => GCStringConverter(typeIdentifier, fieldDef.isList).toGCValue(x).get),
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
      Model(
        id = objectType.name,
        name = objectType.name,
        fields = fields.toList
      )
    }
  }

  lazy val desiredRelations: Set[Relation] = {
    val tmp = for {
      objectType    <- sdl.objectTypes
      relationField <- objectType.relationFields
    } yield {
      Relation(
        id = relationField.relationName.get,
        name = relationField.relationName.get,
        modelAId = objectType.name,
        modelBId = relationField.typeName
      )
    }
    tmp.toSet
  }

  lazy val desiredEnums: Vector[Enum] = {
    sdl.enumTypes.map { enumDef =>
      Enum(
        id = enumDef.name,
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
