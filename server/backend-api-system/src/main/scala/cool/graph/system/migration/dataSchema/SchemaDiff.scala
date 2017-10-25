package cool.graph.system.migration.dataSchema

import sangria.ast.Document

import scala.util.Try

object SchemaDiff {
  def apply(oldSchema: String, newSchema: String): Try[SchemaDiff] = {
    for {
      oldDocParsed <- SdlSchemaParser.parse(oldSchema)
      newDocParsed <- SdlSchemaParser.parse(newSchema)
    } yield SchemaDiff(oldDocParsed, newDocParsed)
  }
}
case class SchemaDiff(
    oldSchema: Document,
    newSchema: Document
) {
  import DataSchemaAstExtensions._

  val addedTypes: Vector[String]   = newSchema.oldTypeNames diff oldSchema.typeNames
  val removedTypes: Vector[String] = oldSchema.typeNames diff newSchema.oldTypeNames

  val updatedTypes: Vector[UpdatedType] = {
    val x = for {
      typeInNewSchema <- newSchema.objectTypes
      typeInOldSchema <- oldSchema.objectTypes.find(_.name == typeInNewSchema.oldName)
    } yield {
      val addedFields = typeInNewSchema.fields.filter(fieldInNewType => typeInOldSchema.fields.forall(_.name != fieldInNewType.oldName))

      val removedFields = typeInOldSchema.fields.filter(fieldInOldType => typeInNewSchema.fields.forall(_.oldName != fieldInOldType.name))

      val updatedFields = (typeInNewSchema.fields diff addedFields).map { updatedField =>
        UpdatedField(updatedField.name, updatedField.oldName, updatedField.fieldType.namedType.name)
      }

      UpdatedType(
        name = typeInNewSchema.name,
        oldName = typeInNewSchema.oldName,
        addedFields = addedFields.map(_.name).toList,
        removedFields = removedFields.map(_.name).toList,
        updatedFields = updatedFields.toList
      )
    }
    x.filter(_.hasChanges)
  }

  val addedEnums: Vector[String]   = newSchema.oldEnumNames diff oldSchema.enumNames
  val removedEnums: Vector[String] = oldSchema.enumNames diff newSchema.oldEnumNames
  val updatedEnums: Vector[UpdatedEnum] = {
    for {
      typeInNewSchema <- newSchema.enumTypes
      typeInOldSchema <- oldSchema.enumTypes.find(_.name == typeInNewSchema.oldName)
    } yield UpdatedEnum(name = typeInNewSchema.name, oldName = typeInOldSchema.name)
  }
}
case class UpdatedType(
    name: String,
    oldName: String,
    addedFields: List[String],
    removedFields: List[String],
    updatedFields: List[UpdatedField]
) {
  def hasChanges: Boolean = addedFields.nonEmpty || removedFields.nonEmpty || updatedFields.nonEmpty
}
case class UpdatedField(
    name: String,
    oldName: String,
    newType: String
)

case class UpdatedEnum(
    name: String,
    oldName: String
)
