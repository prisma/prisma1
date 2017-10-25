package cool.graph.system.database

import cool.graph.cuid.Cuid
import cool.graph.shared.models.{Field, TypeIdentifier}
import scala.util.{Failure, Try}

object SystemFields {
  val idFieldName        = "id"
  val updatedAtFieldName = "updatedAt"
  val createdAtFieldName = "createdAt"
  val systemFieldNames   = Vector(idFieldName, updatedAtFieldName, createdAtFieldName)

  def generateAll: List[Field] = {
    List(
      generateIdField(),
      generateCreatedAtField(),
      generateUpdatedAtField()
    )
  }

  def generateCreatedAtField(id: String = Cuid.createCuid()): Field = {
    Field(
      id = id,
      name = createdAtFieldName,
      typeIdentifier = TypeIdentifier.DateTime,
      isRequired = true,
      isList = false,
      isUnique = false,
      isSystem = true,
      isReadonly = true
    )
  }

  def generateUpdatedAtField(id: String = Cuid.createCuid()): Field = {
    Field(
      id = id,
      name = updatedAtFieldName,
      typeIdentifier = TypeIdentifier.DateTime,
      isRequired = true,
      isList = false,
      isUnique = false,
      isSystem = true,
      isReadonly = true
    )
  }

  def generateIdField(id: String = Cuid.createCuid()): Field = {
    Field(
      id = id,
      name = idFieldName,
      typeIdentifier = TypeIdentifier.GraphQLID,
      isRequired = true,
      isList = false,
      isUnique = true,
      isSystem = true,
      isReadonly = true
    )
  }

  def generateSystemFieldFor(name: String): Field = {
    name match {
      case x if x == idFieldName        => generateIdField()
      case x if x == createdAtFieldName => generateCreatedAtField()
      case x if x == updatedAtFieldName => generateUpdatedAtField()
      case _                            => throw new Exception(s"Unknown system field with name: $name")
    }
  }

  def isDeletableSystemField(name: String)       = name == updatedAtFieldName || name == createdAtFieldName
  def isReservedFieldName(name: String): Boolean = systemFieldNames.contains(name)

  /**
    * Attempts to parse a given field from user input and maps it to the appropriate system field.
    * This is used for "hiding" system fields in the schema initially, like createdAt and updatedAt, which are
    * still in the client database and are recorded all the time, but not exposed for querying in the schema (missing in the project db).
    *
    * If the user chooses to create one of those fields manually, it is then added in the project database, which this util
    * is providing the system fields and verification for.
    */
  def generateSystemFieldFromInput(field: Field): Try[Field] = {
    if (field.name == idFieldName) {
      Failure(new Exception(s"$idFieldName is reserved and can't be created manually."))
    } else if (!field.isRequired || field.isUnique || field.isList || field.typeIdentifier != TypeIdentifier.DateTime) {
      Failure(new Exception(s"Type is predefined and must be non-unique, required, a scalar field (a datetime) to be exposed."))
    } else {
      Try { generateSystemFieldFor(field.name) }
    }
  }
}
