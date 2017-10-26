package cool.graph.client.mutactions.validation

import cool.graph.shared.models._

import scala.util.matching.Regex

object ConstraintValueValidation {

  case class ConstraintError(field: Field, value: Any, constraintType: String, arg: Any)

  def checkConstraintsOnField(f: Field, value: Any): List[ConstraintError] = {
    f.constraints.flatMap { constraint =>
      checkConstraintOnField(f, constraint, value)
    }
  }

  def checkConstraintOnField(f: Field, constraint: FieldConstraint, value: Any): List[ConstraintError] = {
    if (f.isList) {
      val values = value.asInstanceOf[Vector[Any]].toList

      constraint match {
        case constraint: StringConstraint  => values.flatMap(v => checkStringConstraint(f, v, constraint))
        case constraint: NumberConstraint  => values.flatMap(v => checkNumberConstraint(f, v, constraint))
        case constraint: BooleanConstraint => values.flatMap(v => checkBooleanConstraint(f, v, constraint))
        case constraint: ListConstraint    => checkListConstraint(f, values, constraint)
      }
    } else {
      constraint match {
        case constraint: StringConstraint  => checkStringConstraint(f, value, constraint)
        case constraint: NumberConstraint  => checkNumberConstraint(f, value, constraint)
        case constraint: BooleanConstraint => checkBooleanConstraint(f, value, constraint)
        case constraint: ListConstraint    => List(ConstraintError(f, value, "Not a List-Field", ""))
      }
    }
  }

  def checkStringConstraint(f: Field, value: Any, constraint: StringConstraint): List[ConstraintError] = {
    def regexFound(regex: String, value: String): Boolean = { (new Regex(regex) findAllIn value).nonEmpty }

    value match {
      case v: String =>
        val oneOfStringError =
          if (constraint.oneOfString.nonEmpty && !constraint.oneOfString.contains(v))
            List(ConstraintError(f, v, "oneOfString", constraint.oneOfString.toString))
          else List.empty

        oneOfStringError ++ List(
          constraint.equalsString.collect { case x if x != v         => ConstraintError(f, v, "equalsString", x) },
          constraint.minLength.collect { case x if x > v.length      => ConstraintError(f, v, "minLength", x) },
          constraint.maxLength.collect { case x if x < v.length      => ConstraintError(f, v, "maxLength", x) },
          constraint.startsWith.collect { case x if !v.startsWith(x) => ConstraintError(f, v, "startsWith", x) },
          constraint.endsWith.collect { case x if !v.endsWith(x)     => ConstraintError(f, v, "endsWith", x) },
          constraint.includes.collect { case x if !v.contains(x)     => ConstraintError(f, v, "includes", x) },
          constraint.regex.collect { case x if !regexFound(x, v)     => ConstraintError(f, v, "regex", x) }
        ).flatten

      case _ => List(ConstraintError(f, value, "not a String", ""))
    }
  }

  def checkNumberConstraint(field: Field, value: Any, constraint: NumberConstraint): List[ConstraintError] = {
    def checkNumConstraint(f: Field, v: Double): List[ConstraintError] = {
      val oneOfNumberError =
        if (constraint.oneOfNumber.nonEmpty && !constraint.oneOfNumber.contains(v))
          List(ConstraintError(f, v, "oneOfNumber", constraint.oneOfNumber.toString))
        else List.empty

      oneOfNumberError ++ List(
        constraint.equalsNumber.collect { case x if x != v => ConstraintError(f, v, "equalsNumber", x) },
        constraint.min.collect { case x if x > v           => ConstraintError(f, v, "min", x) },
        constraint.max.collect { case x if x < v           => ConstraintError(f, v, "max", x) },
        constraint.exclusiveMin.collect { case x if x >= v => ConstraintError(f, v, "exclusiveMin", x) },
        constraint.exclusiveMax.collect { case x if x <= v => ConstraintError(f, v, "exclusiveMax", x) },
        constraint.multipleOf.collect { case x if v % x != 0 => ConstraintError(f, v, "multipleOf", x) }
      ).flatten
    }

    value match {
      case double: Double => checkNumConstraint(field, double)
      case int: Int       => checkNumConstraint(field, int.asInstanceOf[Double])
      case _              => List(ConstraintError(field, value, "not an Int or Float/Double", ""))
    }
  }

  def checkBooleanConstraint(f: Field, value: Any, constraint: BooleanConstraint): List[ConstraintError] = {
    value match {
      case v: Boolean =>
        List(constraint.equalsBoolean.collect { case x if x != v => ConstraintError(f, v, "equalsBoolean", x) }).flatten
      case _ => List(ConstraintError(f, value, "not a Boolean", ""))
    }
  }

  def checkListConstraint(f: Field, value: Any, constraint: ListConstraint): List[ConstraintError] = {
    def unique(list: List[Any]) = list.toSet.size == list.size

    value match {
      case l: List[Any] =>
        List(
          constraint.uniqueItems.collect { case x if !unique(l) => ConstraintError(f, l, "uniqueItems", "") },
          constraint.minItems.collect { case x if x > l.length  => ConstraintError(f, l, "minItems", x) },
          constraint.maxItems.collect { case x if x < l.length  => ConstraintError(f, l, "maxItems", x) }
        ).flatten
      case _ => List(ConstraintError(f, value, "not a List", ""))
    }
  }
}
