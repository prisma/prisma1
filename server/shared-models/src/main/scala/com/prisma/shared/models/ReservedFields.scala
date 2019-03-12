package com.prisma.shared.models

import com.prisma.shared.models.FieldBehaviour.IdBehaviour

object ReservedFields {
  val idFieldName              = "id"
  val embeddedIdFieldName      = "_id"
  val mongoInternalIdfieldName = "_id"
  val updatedAtFieldName       = "updatedAt"
  val createdAtFieldName       = "createdAt"
  val reservedFieldNames       = Vector(idFieldName, updatedAtFieldName, createdAtFieldName)

  def reservedFieldFor(name: String): FieldTemplate = {
    name match {
      case x if x == idFieldName        => idField
      case x if x == createdAtFieldName => createdAtField
      case x if x == updatedAtFieldName => updatedAtField
      case _                            => throw new Exception(s"Unknown reserved field: $name")
    }
  }

  private val createdAtField: FieldTemplate = {
    FieldTemplate(
      name = createdAtFieldName,
      typeIdentifier = TypeIdentifier.DateTime,
      isRequired = true,
      isList = false,
      isUnique = false,
      isHidden = true,
      enum = None,
      defaultValue = None,
      relationName = None,
      relationSide = None,
      manifestation = None,
      behaviour = None
    )
  }

  private val updatedAtField: FieldTemplate = {
    FieldTemplate(
      name = updatedAtFieldName,
      typeIdentifier = TypeIdentifier.DateTime,
      isRequired = true,
      isList = false,
      isUnique = false,
      isHidden = true,
      enum = None,
      defaultValue = None,
      relationName = None,
      relationSide = None,
      manifestation = None,
      behaviour = None
    )
  }

  private val idField: FieldTemplate = {
    FieldTemplate(
      name = idFieldName,
      typeIdentifier = TypeIdentifier.Cuid,
      isRequired = true,
      isList = false,
      isUnique = true,
      isHidden = true,
      enum = None,
      defaultValue = None,
      relationName = None,
      relationSide = None,
      manifestation = None,
      behaviour = Some(IdBehaviour(strategy = FieldBehaviour.IdStrategy.Auto))
    )
  }

  val embeddedIdField: FieldTemplate = {
    FieldTemplate(
      name = embeddedIdFieldName,
      typeIdentifier = TypeIdentifier.Cuid,
      isRequired = true,
      isList = false,
      isUnique = true,
      isHidden = true,
      enum = None,
      defaultValue = None,
      relationName = None,
      relationSide = None,
      manifestation = None,
      behaviour = Some(IdBehaviour(strategy = FieldBehaviour.IdStrategy.Auto))
    )
  }
}
