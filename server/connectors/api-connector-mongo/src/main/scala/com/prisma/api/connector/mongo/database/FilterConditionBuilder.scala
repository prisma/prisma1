package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.gc_values.{DateTimeGCValue, GCValue, NullGCValue}
import org.mongodb.scala.bson.{BsonDateTime, conversions}
import org.mongodb.scala.model.Filters._

//relationfilters depend on relationtype
// embedded -> use dot notation to go deeper in tree
// nonEmbedded -> not supported, Inputtypes for filter should not be generated in api
//field_every: $all does not work for this purpose
//field_some: $elemMatch
//field_none: $not $elemMatch

trait FilterConditionBuilder {
  def buildConditionForFilter(filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter("", filter)
    case None         => and(hackForTrue)
  }

  private def buildConditionForFilter(path: String, filter: Filter): conversions.Bson = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => and(hackForTrue)
      case AndFilter(filters)     => and(nonEmptyConditions(path, filters): _*)
      case OrFilter(filters)      => or(nonEmptyConditions(path, filters): _*)
      case NotFilter(filters)     => not(and(filters.map(f => buildConditionForFilter(path, f)): _*))
      case NodeFilter(filters)    => buildConditionForFilter(path, OrFilter(filters))
      case x: RelationFilter      => relationFilterStatement(path, x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                                            => and(hackForTrue)
      case FalseFilter                                           => not(and(hackForTrue))
      case ScalarFilter(scalarField, Contains(value))            => regex(combineTwo(path, scalarField.name), value.value.toString)
      case ScalarFilter(scalarField, NotContains(value))         => not(regex(combineTwo(path, scalarField.name), value.value.toString))
      case ScalarFilter(scalarField, StartsWith(value))          => regex(combineTwo(path, scalarField.name), "^" + value.value + ".*")
      case ScalarFilter(scalarField, NotStartsWith(value))       => not(regex(combineTwo(path, scalarField.name), "^" + value.value + ".*"))
      case ScalarFilter(scalarField, EndsWith(value))            => regex(combineTwo(path, scalarField.name), value.value + "$")
      case ScalarFilter(scalarField, NotEndsWith(value))         => not(regex(combineTwo(path, scalarField.name), value.value + "$"))
      case ScalarFilter(scalarField, LessThan(value))            => lt(combineTwo(path, scalarField.name), fromGCValue(value))
      case ScalarFilter(scalarField, GreaterThan(value))         => gt(combineTwo(path, scalarField.name), fromGCValue(value))
      case ScalarFilter(scalarField, LessThanOrEquals(value))    => lte(combineTwo(path, scalarField.name), fromGCValue(value))
      case ScalarFilter(scalarField, GreaterThanOrEquals(value)) => gte(combineTwo(path, scalarField.name), fromGCValue(value))
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => notEqual(combineTwo(path, scalarField.name), null)
      case ScalarFilter(scalarField, NotEquals(value))           => notEqual(combineTwo(path, scalarField.name), fromGCValue(value))
      case ScalarFilter(scalarField, Equals(NullGCValue))        => equal(combineTwo(path, scalarField.name), null)
      case ScalarFilter(scalarField, Equals(value))              => equal(combineTwo(path, scalarField.name), fromGCValue(value))
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => in(combineTwo(path, scalarField.name), null)
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => not(in(combineTwo(path, scalarField.name), null))
      case ScalarFilter(scalarField, In(values))                 => in(combineTwo(path, scalarField.name), values.map(fromGCValue): _*)
      case ScalarFilter(scalarField, NotIn(values))              => not(in(combineTwo(path, scalarField.name), values.map(fromGCValue): _*))
      case OneRelationIsNullFilter(field)                        => equal(combineTwo(path, field.name), null)
      case x                                                     => sys.error(s"Not supported: $x")
    }
  }
  def nonEmptyConditions(path: String, filters: Vector[Filter]): Vector[conversions.Bson] = filters.map(f => buildConditionForFilter(path, f)) match {
    case x if x.isEmpty => Vector(and(hackForTrue))
    case x              => x
  }
  def fromGCValue(value: GCValue): Any = value match {
    case DateTimeGCValue(value) => BsonDateTime(value.getMillis)
    case x: GCValue             => x.value
  }
  val hackForTrue = notEqual("_id", -1)

  //does elem Match eliminate the need for the path???

  private def relationFilterStatement(path: String, relationFilter: RelationFilter) = {
    val toOneNested  = buildConditionForFilter(combineTwo(path, relationFilter.field.name), relationFilter.nestedFilter)
    val toManyNested = buildConditionForFilter("", relationFilter.nestedFilter)

    relationFilter.condition match {
      case AtLeastOneRelatedNode => elemMatch(relationFilter.field.name, toManyNested)
      case EveryRelatedNode      => not(elemMatch(relationFilter.field.name, not(toManyNested)))
      case NoRelatedNode         => not(elemMatch(relationFilter.field.name, toManyNested))
      case ToOneRelatedNode      => toOneNested
    }
  }

}
