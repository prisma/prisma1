package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.gc_values.{DateTimeGCValue, GCValue, NullGCValue}
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters._

//relationfilters depend on relationtype
// embedded -> use dot notation to go deeper in tree
// nonEmbedded -> not supported, Inputtypes for filter should not be generated in api
//field_every: $all does not work for this purpose
//field_some: $elemMatch
//field_none: $not $elemMatch

trait FilterConditionBuilder {
  def buildConditionForFilter(filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter(filter)
    case None         => and(hackForTrue)
  }

  private def buildConditionForFilter(filter: Filter): conversions.Bson = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => and(hackForTrue)
      case AndFilter(filters)     => and(nonEmptyConditions(filters): _*)
      case OrFilter(filters)      => or(nonEmptyConditions(filters): _*)
      case NotFilter(filters)     => not(and(filters.map(f => buildConditionForFilter(f)): _*))
      case NodeFilter(filters)    => buildConditionForFilter(OrFilter(filters))
      case x: RelationFilter      => relationFilterStatement(x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                                            => and(hackForTrue)
      case FalseFilter                                           => not(and(hackForTrue))
      case ScalarFilter(scalarField, Contains(value))            => regex(scalarField.name, s".*${value.value}.*")
      case ScalarFilter(scalarField, NotContains(value))         => not(regex(scalarField.name, s".*${value.value}.*"))
      case ScalarFilter(scalarField, StartsWith(value))          => regex(scalarField.name, s"${value.value}.*")
      case ScalarFilter(scalarField, NotStartsWith(value))       => not(regex(scalarField.name, s"${value.value}.*"))
      case ScalarFilter(scalarField, EndsWith(value))            => regex(scalarField.name, s".*${value.value}")
      case ScalarFilter(scalarField, NotEndsWith(value))         => not(regex(scalarField.name, s".*${value.value}"))
      case ScalarFilter(scalarField, LessThan(value))            => lt(scalarField.name, fromGCValue(value))
      case ScalarFilter(scalarField, GreaterThan(value))         => gt(scalarField.name, fromGCValue(value))
      case ScalarFilter(scalarField, LessThanOrEquals(value))    => lte(scalarField.name, fromGCValue(value))
      case ScalarFilter(scalarField, GreaterThanOrEquals(value)) => gte(scalarField.name, fromGCValue(value))
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => notEqual(scalarField.name, null)
      case ScalarFilter(scalarField, NotEquals(value))           => notEqual(scalarField.name, fromGCValue(value))
      case ScalarFilter(scalarField, Equals(NullGCValue))        => equal(scalarField.name, null)
      case ScalarFilter(scalarField, Equals(value))              => equal(scalarField.name, fromGCValue(value))
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => in(scalarField.name, null)
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => not(in(scalarField.name, null))
      case ScalarFilter(scalarField, In(values))                 => in(scalarField.name, values.map(fromGCValue): _*)
      case ScalarFilter(scalarField, NotIn(values))              => not(in(scalarField.name, values.map(fromGCValue): _*))
      case OneRelationIsNullFilter(field)                        => equal(field.name, null)
      case x                                                     => sys.error(s"Not supported: $x")
    }
  }
  def nonEmptyConditions(filters: Vector[Filter]): Vector[conversions.Bson] = filters.map(buildConditionForFilter) match {
    case x if x.isEmpty => Vector(and(hackForTrue))
    case x              => x
  }
  def fromGCValue(value: GCValue): Any = value match {
    case DateTimeGCValue(value) => value.getMillis
    case x: GCValue             => x.value
  }
  val hackForTrue = notEqual("_id", -1)

  private def relationFilterStatement(relationFilter: RelationFilter) = relationFilter.condition match {
    case AtLeastOneRelatedNode => elemMatch(relationFilter.field.name, buildConditionForFilter(relationFilter.nestedFilter))
    case EveryRelatedNode      => not(elemMatch(relationFilter.field.name, not(buildConditionForFilter(relationFilter.nestedFilter))))
    case NoRelatedNode         => not(elemMatch(relationFilter.field.name, buildConditionForFilter(relationFilter.nestedFilter)))
    case NoRelationCondition   => elemMatch(relationFilter.field.name, buildConditionForFilter(relationFilter.nestedFilter))
  }

}
