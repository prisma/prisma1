package cool.graph.deploy.migration

import cool.graph.cuid.Cuid
import cool.graph.shared.models.{Field, TypeIdentifier}

object ReservedFields {
  val idFieldName        = "id"
  val updatedAtFieldName = "updatedAt"
  val createdAtFieldName = "createdAt"
  val reservedFieldNames = Vector(idFieldName, updatedAtFieldName, createdAtFieldName)

  def generateAll: List[Field] = {
    List(
      idField(),
      createdAtField(),
      updatedAtField()
    )
  }

  def createdAtField(id: String = Cuid.createCuid()): Field = {
    Field(
      id = createdAtFieldName,
      name = createdAtFieldName,
      typeIdentifier = TypeIdentifier.DateTime,
      isRequired = true,
      isList = false,
      isUnique = false,
      isReadonly = true,
      enum = None,
      defaultValue = None,
      relation = None,
      relationSide = None
    )
  }

  def updatedAtField(id: String = Cuid.createCuid()): Field = {
    Field(
      id = updatedAtFieldName,
      name = updatedAtFieldName,
      typeIdentifier = TypeIdentifier.DateTime,
      isRequired = true,
      isList = false,
      isUnique = false,
      isReadonly = true,
      enum = None,
      defaultValue = None,
      relation = None,
      relationSide = None
    )
  }

  def idField(id: String = Cuid.createCuid()): Field = {
    Field(
      id = idFieldName,
      name = idFieldName,
      typeIdentifier = TypeIdentifier.GraphQLID,
      isRequired = true,
      isList = false,
      isUnique = true,
      isReadonly = true,
      enum = None,
      defaultValue = None,
      relation = None,
      relationSide = None
    )
  }

  def reservedFieldFor(name: String): Field = {
    name match {
      case x if x == idFieldName        => idField()
      case x if x == createdAtFieldName => createdAtField()
      case x if x == updatedAtFieldName => updatedAtField()
      case _                            => throw new Exception(s"Unknown reserved field: $name")
    }
  }

  def isReservedFieldName(name: String): Boolean = reservedFieldNames.contains(name)
}
