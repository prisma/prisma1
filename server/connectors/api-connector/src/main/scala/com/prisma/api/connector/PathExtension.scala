package com.prisma.api.connector

import com.prisma.shared.models._

object Path {
  def empty = Path(List.empty)
}

case class Path(segments: List[PathSegment]) {
  def append(rF: RelationField, where: NodeSelector): Path            = this.copy(segments = this.segments :+ ToManySegment(rF, where))
  def append(rF: RelationField, whereFilter: Option[Filter]): Path    = this.copy(segments = this.segments :+ ToManyFilterSegment(rF, whereFilter))
  def append(rF: RelationField): Path                                 = this.copy(segments = this.segments :+ ToOneSegment(rF))
  def stringForField(field: String): String                           = stringGen(field, segments).mkString(".")
  def operatorName(field: RelationField, where: NodeSelector)         = s"${field.name}X${where.fieldName}X${where.hashCode().toString.replace("-", "M")}"
  def operatorName(field: RelationField, whereFilter: Option[Filter]) = s"${field.name}X${whereFilter.hashCode().toString.replace("-", "M")}"

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
