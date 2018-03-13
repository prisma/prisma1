package com.prisma.shared.models

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

  def createdAtField(): Field = {
    Field(
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

  def updatedAtField(): Field = {
    Field(
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

  def idField(): Field = {
    Field(
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
}
