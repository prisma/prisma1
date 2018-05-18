package com.prisma.shared.models

import com.prisma.shared.models.FieldConstraintType.FieldConstraintType

sealed trait FieldConstraint {
  val id: String; val fieldId: String; val constraintType: FieldConstraintType
}

case class StringConstraint(
    id: String,
    fieldId: String,
    equalsString: Option[String] = None,
    oneOfString: List[String] = List.empty,
    minLength: Option[Int] = None,
    maxLength: Option[Int] = None,
    startsWith: Option[String] = None,
    endsWith: Option[String] = None,
    includes: Option[String] = None,
    regex: Option[String] = None
) extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.STRING
}

case class NumberConstraint(
    id: String,
    fieldId: String,
    equalsNumber: Option[Double] = None,
    oneOfNumber: List[Double] = List.empty,
    min: Option[Double] = None,
    max: Option[Double] = None,
    exclusiveMin: Option[Double] = None,
    exclusiveMax: Option[Double] = None,
    multipleOf: Option[Double] = None
) extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.NUMBER
}

case class BooleanConstraint(id: String, fieldId: String, equalsBoolean: Option[Boolean] = None) extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.BOOLEAN
}

case class ListConstraint(id: String, fieldId: String, uniqueItems: Option[Boolean] = None, minItems: Option[Int] = None, maxItems: Option[Int] = None)
    extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.LIST
}

object FieldConstraintType extends Enumeration {
  type FieldConstraintType = Value
  val STRING  = Value("STRING")
  val NUMBER  = Value("NUMBER")
  val BOOLEAN = Value("BOOLEAN")
  val LIST    = Value("LIST")
}
