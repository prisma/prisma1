package cool.graph.deploy.migration

import sangria.ast.Document

trait RenameInferer {
  def infer(graphQlSdl: Document): Renames
}

object RenameInferer extends RenameInferer {
  import DataSchemaAstExtensions._

  override def infer(graphQlSdl: Document): Renames = {
    val modelNameMapping: Map[String, String] = graphQlSdl.objectTypes.map { objectType =>
      objectType.oldName -> objectType.name
    }.toMap

    val enumNameMapping: Map[String, String] = graphQlSdl.enumTypes.map { enumType =>
      enumType.oldName -> enumType.name
    }.toMap

    val fieldNameMapping: Map[String, String] = {
      for {
        objectType <- graphQlSdl.objectTypes
        fieldDef   <- objectType.fields
      } yield s"${objectType.oldName}.${fieldDef.oldName}" -> fieldDef.name
    }.toMap

    Renames(
      models = modelNameMapping,
      enums = enumNameMapping,
      fields = fieldNameMapping
    )
  }

}
