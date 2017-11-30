package cool.graph.deploy.migration

import sangria.ast.Document

trait RenameInferer {
  def infer(graphQlSdl: Document): Renames
}

// todo doesnt infer a thing - naming is off
// todo mapping might be insufficient for edge cases: Model renamed, field on model renamed as well
object RenameInferer extends RenameInferer {
  import DataSchemaAstExtensions._

  // Mapping is from the next (== new) name to the previous name. The name can only be different if there is an @rename directive present.
  override def infer(graphQlSdl: Document): Renames = {
    val modelRenames: Vector[Rename] = graphQlSdl.objectTypes.map { objectType =>
      Rename(previous = objectType.previousName, next = objectType.name)
    }

    val enumRenames: Vector[Rename] = graphQlSdl.enumTypes.map { enumType =>
      Rename(previous = enumType.previousName, next = enumType.name)
    }

    val fieldRenames: Vector[FieldRename] =
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

    Renames(
      models = modelRenames,
      enums = enumRenames,
      fields = fieldRenames
    )
  }
}
