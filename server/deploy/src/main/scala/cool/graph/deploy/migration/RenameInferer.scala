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
    val modelNameMapping: Map[String, String] = graphQlSdl.objectTypes.map { objectType =>
      objectType.name -> objectType.previousName
    }.toMap

    val enumNameMapping: Map[String, String] = graphQlSdl.enumTypes.map { enumType =>
      enumType.name -> enumType.previousName
    }.toMap

    val fieldNameMapping: Map[(String, String), String] = {
      for {
        objectType <- graphQlSdl.objectTypes
        fieldDef   <- objectType.fields
      } yield (objectType.previousName, fieldDef.previousName) -> fieldDef.name
    }.toMap

    Renames(
      models = modelNameMapping,
      enums = enumNameMapping,
      fields = fieldNameMapping
    )
  }
}
