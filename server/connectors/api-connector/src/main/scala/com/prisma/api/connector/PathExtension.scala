package com.prisma.api.connector

import com.prisma.shared.models._

object Path {
  def empty = Path(List.empty)
}

case class Path(segments: List[PathSegment]) {
  def append(rF: RelationField, where: NodeSelector): Path                    = this.copy(segments = this.segments :+ ToManySegment(rF, where))
  def append(rF: RelationField, whereFilter: Option[Filter]): Path            = this.copy(segments = this.segments :+ ToManyFilterSegment(rF, whereFilter))
  def append(rF: RelationField): Path                                         = this.copy(segments = this.segments :+ ToOneSegment(rF))
  def stringForField(f: String, model: Model): String                         = stringForField(model.getFieldByName_!(f).dbName)
  def stringForField(field: String): String                                   = stringGen(field, segments).mkString(".")
  def operatorName(field: RelationField, where: NodeSelector): String         = sanitize(s"${field.name}X${where.fieldName}X${where.hashCode().toString}")
  def operatorName(field: RelationField, whereFilter: Option[Filter]): String = sanitize(s"${field.name}X${whereFilter.hashCode().toString}")
  def dropLast: Path                                                          = this.copy(segments = this.segments.dropRight(1))
  def dropFirst: Path                                                         = this.copy(segments = this.segments.drop(1))
  def combinedNames                                                           = this.segments.map(_.rf.name).mkString(".")
  private def sanitize(input: String): String = {
    //Mongo only allows alphanumeric characters in arrayfilter names and they have to start with lowercase
    val alphanumeric = input
      .replace("-", "M") // for the minus in hash value
      .replace("_", "X") // for the _ in _id

    "x" + alphanumeric
  }

  def selectedFields(field: RelationField): SelectedFields = selectedFieldsHelper(field, this.segments)
  private def selectedFieldsHelper(field: RelationField, segments: List[PathSegment]): SelectedFields = segments match {
    case Nil                                => SelectedFields(Set(SelectedRelationField(field, SelectedFields.empty)))
    case ToOneSegment(rf) :: tail           => SelectedFields(Set(SelectedRelationField(rf, selectedFieldsHelper(field, tail))))
    case ToManySegment(rf, where) :: tail   => SelectedFields(Set(SelectedScalarField(where.field), SelectedRelationField(rf, selectedFieldsHelper(field, tail))))
    case ToManyFilterSegment(rf, _) :: tail => SelectedFields(Set(SelectedRelationField(rf, selectedFieldsHelper(field, tail)))) //Fixme
  }

  private def stringGen(field: String, segments: List[PathSegment]): Vector[String] = segments match {
    case Nil                                          => Vector(field)
    case ToOneSegment(rf) :: tail                     => rf.name +: stringGen(field, tail)
    case ToManySegment(rf, where) :: tail             => Vector(rf.name, "$[" + operatorName(rf, where) + "]") ++ stringGen(field, tail)
    case ToManyFilterSegment(rf, whereFilter) :: tail => Vector(rf.name, "$[" + operatorName(rf, whereFilter) + "]") ++ stringGen(field, tail)
  }
}

sealed trait PathSegment {
  def rf: RelationField
}

case class ToOneSegment(rf: RelationField)                                     extends PathSegment
case class ToManySegment(rf: RelationField, where: NodeSelector)               extends PathSegment
case class ToManyFilterSegment(rf: RelationField, whereFilter: Option[Filter]) extends PathSegment
