package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.HackforTrue.hackForTrue
import com.prisma.gc_values.{GCValue, NullGCValue}
import com.prisma.shared.models.ScalarField
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters._

//relationfilters depend on relationtype
// embedded -> use dot notation to go deeper in tree
// nonEmbedded -> not supported, Inputtypes for filter should not be generated in api
//field_every:  $not $elemMatch ($not nested)
//field_some:   $elemMatch (nested)
//field_none:   $not $elemMatch (nested)

trait FilterConditionBuilder {
  def buildConditionForFilter(filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter("", filter)
    case None         => hackForTrue
  }

  def buildConditionForScalarFilter(operator: String, filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter(operator, filter)
    case None         => hackForTrue
  }

  private def buildConditionForFilter(path: String, filter: Filter): conversions.Bson = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => hackForTrue
      case AndFilter(filters)     => and(nonEmptyConditions(path, filters): _*)
      case OrFilter(filters)      => or(nonEmptyConditions(path, filters): _*)
      case NotFilter(filters)     => nor(filters.map(f => buildConditionForFilter(path, f)): _*) //not can only negate equality comparisons not filters
      case NodeFilter(filters)    => buildConditionForFilter(path, OrFilter(filters))
      case x: RelationFilter      => relationFilterStatement(path, x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                                            => hackForTrue
      case FalseFilter                                           => not(hackForTrue)
      case ScalarFilter(scalarField, Contains(value))            => regex(dotPath(path, scalarField), value.value.toString)
      case ScalarFilter(scalarField, NotContains(value))         => not(regex(dotPath(path, scalarField), value.value.toString))
      case ScalarFilter(scalarField, StartsWith(value))          => regex(dotPath(path, scalarField), "^" + value.value)
      case ScalarFilter(scalarField, NotStartsWith(value))       => not(regex(dotPath(path, scalarField), "^" + value.value))
      case ScalarFilter(scalarField, EndsWith(value))            => regex(dotPath(path, scalarField), value.value + "$")
      case ScalarFilter(scalarField, NotEndsWith(value))         => not(regex(dotPath(path, scalarField), value.value + "$"))
      case ScalarFilter(scalarField, LessThan(value))            => lt(dotPath(path, scalarField), GCToBson(value))
      case ScalarFilter(scalarField, GreaterThan(value))         => gt(dotPath(path, scalarField), GCToBson(value))
      case ScalarFilter(scalarField, LessThanOrEquals(value))    => lte(dotPath(path, scalarField), GCToBson(value))
      case ScalarFilter(scalarField, GreaterThanOrEquals(value)) => gte(dotPath(path, scalarField), GCToBson(value))
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => notEqual(dotPath(path, scalarField), null)
      case ScalarFilter(scalarField, NotEquals(value))           => notEqual(dotPath(path, scalarField), GCToBson(value))
      case ScalarFilter(scalarField, Equals(NullGCValue))        => equal(dotPath(path, scalarField), null)
      case ScalarFilter(scalarField, Equals(value))              => equal(dotPath(path, scalarField), GCToBson(value))
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => in(dotPath(path, scalarField), null)
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => not(in(dotPath(path, scalarField), null))
      case ScalarFilter(scalarField, In(values))                 => in(dotPath(path, scalarField), values.map(GCToBson(_)): _*)
      case ScalarFilter(scalarField, NotIn(values))              => not(in(dotPath(path, scalarField), values.map(GCToBson(_)): _*))
      //Fixme test this thoroughly
      case ScalarListFilter(scalarListField, ListContains(value))       => all(dotPath(path, scalarListField), GCToBson(value))
      case ScalarListFilter(scalarListField, ListContainsSome(values))  => or(values.map(value => all(dotPath(path, scalarListField), GCToBson(value))): _*)
      case ScalarListFilter(scalarListField, ListContainsEvery(values)) => all(dotPath(path, scalarListField), values.map(GCToBson(_)): _*)
      case OneRelationIsNullFilter(field)                               => equal(dotPath(path, field), null)
      case x                                                            => sys.error(s"Not supported: $x")
    }
  }

  def nonEmptyConditions(path: String, filters: Vector[Filter]): Vector[conversions.Bson] = filters.map(f => buildConditionForFilter(path, f)) match {
    case x if x.isEmpty && path == "" => Vector(hackForTrue)
    case x if x.isEmpty               => Vector(notEqual(s"$path._id", -1))
    case x                            => x
  }

  private def relationFilterStatement(path: String, relationFilter: RelationFilter) = {
    val toOneNested  = buildConditionForFilter(dotPath(path, relationFilter.field), relationFilter.nestedFilter)
    val toManyNested = buildConditionForFilter("", relationFilter.nestedFilter)

    relationFilter.condition match {
      case AtLeastOneRelatedNode => elemMatch(relationFilter.field.dbName, toManyNested)
      case EveryRelatedNode      => not(elemMatch(relationFilter.field.dbName, not(toManyNested)))
      case NoRelatedNode         => not(elemMatch(relationFilter.field.dbName, toManyNested))
      case ToOneRelatedNode      => toOneNested
    }
  }
}
