package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.HackforTrue.hackForTrue
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.ScalarField
import org.mongodb.scala.bson.collection.mutable.Document
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
    case None         => Document()
  }

  def buildConditionForScalarFilter(operator: String, filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter(operator, filter)
    case None         => Document()
  }

  private def buildConditionForFilter(path: String, filter: Filter, negate: Boolean = false): conversions.Bson = {
    val convertedFilter = filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter       => hackForTrue
      case AndFilter(filters) if negate => or(nonEmptyConditions(path, filters, negate): _*)
      case AndFilter(filters)           => and(nonEmptyConditions(path, filters, negate): _*)
      case OrFilter(filters)            => sys.error("These should not be hit ")
      case NotFilter(filters)           => sys.error("These should not be hit ")
      case x: RelationFilter            => relationFilterStatement(path, x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                                            => hackForTrue
      case FalseFilter                                           => not(hackForTrue)
      case ScalarFilter(scalarField, Contains(value))            => regex(combineTwo(path, renameId(scalarField)), value.value.toString)
      case ScalarFilter(scalarField, NotContains(value))         => not(regex(combineTwo(path, renameId(scalarField)), value.value.toString))
      case ScalarFilter(scalarField, StartsWith(value))          => regex(combineTwo(path, renameId(scalarField)), "^" + value.value)
      case ScalarFilter(scalarField, NotStartsWith(value))       => not(regex(combineTwo(path, renameId(scalarField)), "^" + value.value))
      case ScalarFilter(scalarField, EndsWith(value))            => regex(combineTwo(path, renameId(scalarField)), value.value + "$")
      case ScalarFilter(scalarField, NotEndsWith(value))         => not(regex(combineTwo(path, renameId(scalarField)), value.value + "$"))
      case ScalarFilter(scalarField, LessThan(value))            => lt(combineTwo(path, renameId(scalarField)), GCToBson(value))
      case ScalarFilter(scalarField, GreaterThan(value))         => gt(combineTwo(path, renameId(scalarField)), GCToBson(value))
      case ScalarFilter(scalarField, LessThanOrEquals(value))    => lte(combineTwo(path, renameId(scalarField)), GCToBson(value))
      case ScalarFilter(scalarField, GreaterThanOrEquals(value)) => gte(combineTwo(path, renameId(scalarField)), GCToBson(value))
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => notEqual(combineTwo(path, renameId(scalarField)), null)
      case ScalarFilter(scalarField, NotEquals(value))           => notEqual(combineTwo(path, renameId(scalarField)), GCToBson(value))
      case ScalarFilter(scalarField, Equals(NullGCValue))        => equal(combineTwo(path, renameId(scalarField)), null)
      case ScalarFilter(scalarField, Equals(value))              => equal(combineTwo(path, renameId(scalarField)), GCToBson(value))
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => in(combineTwo(path, renameId(scalarField)), null)
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => not(in(combineTwo(path, renameId(scalarField)), null))
      case ScalarFilter(scalarField, In(values))                 => in(combineTwo(path, renameId(scalarField)), values.map(GCToBson(_)): _*)
      case ScalarFilter(scalarField, NotIn(values))              => not(in(combineTwo(path, renameId(scalarField)), values.map(GCToBson(_)): _*))
      //Fixme test this thoroughly
      case ScalarListFilter(scalarListField, ListContains(value)) => all(combineTwo(path, renameId(scalarListField)), GCToBson(value))
      case ScalarListFilter(scalarListField, ListContainsSome(values)) =>
        or(values.map(value => all(combineTwo(path, renameId(scalarListField)), GCToBson(value))): _*)
      case ScalarListFilter(scalarListField, ListContainsEvery(values)) => all(combineTwo(path, renameId(scalarListField)), values.map(GCToBson(_)): _*)
      case OneRelationIsNullFilter(field)                               => equal(combineTwo(path, field.name), null)
      case x                                                            => sys.error(s"Not supported: $x")
    }

    (filter, negate) match {
      case (AndFilter(_), _) => convertedFilter
      case (_, true)         => not(convertedFilter)
      case (_, false)        => convertedFilter
    }

  }

  def renameId(field: ScalarField): String = if (field.isId) "_id" else field.dbName

  private def nonEmptyConditions(path: String, filters: Vector[Filter], negate: Boolean): Vector[conversions.Bson] =
    filters.map(f => buildConditionForFilter(path, f, negate)) match {
      case x if x.isEmpty && path == "" => Vector(hackForTrue)
      case x if x.isEmpty               => Vector(notEqual(s"$path._id", -1))
      case x                            => x
    }

  private def relationFilterStatement(path: String, relationFilter: RelationFilter) = {
    val fieldName = if (relationFilter.field.relatedModel_!.isEmbedded) relationFilter.field.dbName else relationFilter.field.name

    relationFilter.condition match {
      case AtLeastOneRelatedNode => elemMatch(fieldName, buildConditionForFilter("", relationFilter.nestedFilter))
      case EveryRelatedNode      => not(elemMatch(fieldName, buildConditionForFilter("", relationFilter.nestedFilter, true)))
      case NoRelatedNode         => not(elemMatch(fieldName, buildConditionForFilter("", relationFilter.nestedFilter)))
      case ToOneRelatedNode      => buildConditionForFilter(combineTwo(path, fieldName), relationFilter.nestedFilter)
    }
  }
}
