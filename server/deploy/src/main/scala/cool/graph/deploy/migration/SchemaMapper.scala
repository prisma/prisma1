package cool.graph.deploy.migration

import cool.graph.deploy.migration.inference.{FieldRename, Rename, SchemaMapping}
import sangria.ast.Document

trait SchemaMapper {
  def createMapping(graphQlSdl: Document): SchemaMapping
}

// todo mapping might be insufficient for edge cases: Model renamed, field on model renamed as well
object SchemaMapper extends SchemaMapper {
  import DataSchemaAstExtensions._

  // Mapping is from the next (== new) name to the previous name. The name can only be different if there is an @rename directive present.
  override def createMapping(graphQlSdl: Document): SchemaMapping = {
    val modelMapping: Vector[Rename] = graphQlSdl.objectTypes.map { objectType =>
      Rename(previous = objectType.previousName, next = objectType.name)
    }

    val enumMapping: Vector[Rename] = graphQlSdl.enumTypes.map { enumType =>
      Rename(previous = enumType.previousName, next = enumType.name)
    }

    val fieldMapping: Vector[FieldRename] =
      for {
        objectType <- graphQlSdl.objectTypes
        fieldDef   <- objectType.fields
      } yield {
        FieldRename(
          previousModel = objectType.previousName,
          previousField = fieldDef.previousName,
          nextModel = objectType.name,
          nextField = fieldDef.name
        )
      }

    inference.SchemaMapping(
      models = modelMapping,
      enums = enumMapping,
      fields = fieldMapping
    )
  }
}
