package com.prisma.api.connector

import com.prisma.shared.models._

object Path {
  def empty = Path(List.empty)
}

case class Path(segments: List[PathSegment]) {
  def append(rF: RelationField, where: NodeSelector): Path = this.copy(segments = this.segments :+ ToManySegment(rF, where))
  def append(rF: RelationField): Path                      = this.copy(segments = this.segments :+ ToOneSegment(rF))

//  def string: String = stringGen(segments).mkString(".")

  private def stringGen(segments: List[PathSegment]): Vector[String] = segments match {
    case Nil                          => Vector.empty
    case ToOneSegment(rf) :: tail     => rf.name +: stringGen(tail)
    case ToManySegment(rf, _) :: tail => rf.name +: stringGen(tail)
  }

  def stringForField(field: String): String = stringGen2(field, segments).mkString(".")

  private def stringGen2(field: String, segments: List[PathSegment]): Vector[String] = segments match {
    case Nil                              => Vector(field)
    case ToOneSegment(rf) :: tail         => rf.name +: stringGen2(field, tail)
    case ToManySegment(rf, where) :: tail => Vector(rf.name, "$[" + operatorName(rf, where) + "]") ++ stringGen2(field, tail)
  }

  def operatorName(field: RelationField, where: NodeSelector) = s"${field.name}X${where.fieldName}X${where.hashCode().toString.replace("-", "M")}"
}

sealed trait PathSegment {
  def rf: RelationField
}

case class ToOneSegment(rf: RelationField)                       extends PathSegment
case class ToManySegment(rf: RelationField, where: NodeSelector) extends PathSegment
