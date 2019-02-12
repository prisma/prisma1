package com.prisma.deploy.migration.inference

object SchemaMapping {
  val empty = SchemaMapping()
}

case class SchemaMapping(
    models: Vector[Mapping] = Vector.empty,
    enums: Vector[Mapping] = Vector.empty,
    fields: Vector[FieldMapping] = Vector.empty,
    relations: Vector[Mapping] = Vector.empty
) {
  def getPreviousModelName(nextModel: String): String = models.find(_.next == nextModel).map(_.previous).getOrElse(nextModel)
  def getPreviousEnumName(nextEnum: String): String   = enums.find(_.next == nextEnum).map(_.previous).getOrElse(nextEnum)
  def getPreviousFieldName(nextModel: String, nextField: String): String =
    fields.find(r => r.nextModel == nextModel && r.nextField == nextField).map(_.previousField).getOrElse(nextField)
  def getPreviousRelationName(nextRelation: String): String = relations.find(_.next == nextRelation).map(_.previous).getOrElse(nextRelation)

  def wasModelRenamed(nextModel: String) = getPreviousModelName(nextModel) != nextModel

  def getNextModelName(previousModel: String): String = models.find(_.previous == previousModel).map(_.next).getOrElse(previousModel)
  def getNextEnumName(previousEnum: String): String   = enums.find(_.previous == previousEnum).map(_.next).getOrElse(previousEnum)
  def getNextFieldName(previousModel: String, previousField: String): String =
    fields.find(r => r.previousModel == previousModel && r.previousField == previousField).map(_.nextField).getOrElse(previousField)
  def getNextRelationName(previousRelation: String): String = relations.find(_.previous == previousRelation).map(_.next).getOrElse(previousRelation)

}

case class Mapping(previous: String, next: String)
case class FieldMapping(previousModel: String, previousField: String, nextModel: String, nextField: String)
