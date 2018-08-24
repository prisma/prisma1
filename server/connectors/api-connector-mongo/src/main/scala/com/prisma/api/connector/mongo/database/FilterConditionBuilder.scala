package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.gc_values.{DateTimeGCValue, GCValue, NullGCValue}
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters._

//relationfilters depend on relationtype
// embedded -> use dot notation to go deeper in tree
// nonEmbedded -> not supported, Inputtypes should not be generated
//field_every: $all
//field_some: $elemMatch
//field_none: $not $elemMatch

trait FilterConditionBuilder {
  def buildConditionForFilter(filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter(filter)
    case None         => and(hackForTrue)
  }

  private def buildConditionForFilter(filter: Filter): conversions.Bson = {
    def nonEmptyConditions(filters: Vector[Filter]): Vector[conversions.Bson] = {
      filters.map(buildConditionForFilter) match {
        case x if x.isEmpty => Vector(and(hackForTrue))
        case x              => x
      }
    }

    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => and(hackForTrue)
      case AndFilter(filters)     => and(nonEmptyConditions(filters): _*)
      case OrFilter(filters)      => or(nonEmptyConditions(filters): _*)
      case NotFilter(filters)     => not(and(filters.map(buildConditionForFilter): _*))
      case NodeFilter(filters)    => buildConditionForFilter(OrFilter(filters))
      case x: RelationFilter      => and(hackForTrue) //relationFilterStatement(alias, x, relField, invert)

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
      case OneRelationIsNullFilter(field)                        => and(hackForTrue) //oneRelationIsNullFilter(field, alias)
      case x                                                     => sys.error(s"Not supported: $x")
    }
  }

  def fromGCValue(value: GCValue): Any = value match {
    case DateTimeGCValue(value) => value.getMillis
    case x: GCValue             => x.value
  }
  val hackForTrue = notEqual("_id", -1)

  //Fixme
//  private def relationFilterStatement(alias: String, relationFilter: RelationFilter, relField: Option[Field[AnyRef]], invert: Boolean): Condition = {
//
//
//    val relationField = relationFilter.field
//    val relation      = relationField.relation
//    val newAlias      = relationField.relatedModel_!.dbName + "_" + alias
//
//    relationFilter.nestedFilter match {
//      case x: RelationFilter =>
//        val relField = Some(relationColumn(relation, relationField.oppositeRelationSide))
//        val select = sql
//          .select(relationColumn(relation, relationField.relationSide))
//          .from(relationTable(relation))
//
//        relationFilter.condition match {
//          case AtLeastOneRelatedNode => modelIdColumn(alias, relationField.model).in(select.where(buildConditionForFilter(x, newAlias, relField)))
//          case EveryRelatedNode      => modelIdColumn(alias, relationField.model).notIn(select.where(buildConditionForFilter(x, newAlias, relField, invert = true)))
//          case NoRelatedNode         => modelIdColumn(alias, relationField.model).notIn(select.where(buildConditionForFilter(x, newAlias, relField)))
//          case NoRelationCondition   => modelIdColumn(alias, relationField.model).in(select.where(buildConditionForFilter(x, newAlias, relField)))
//        }
//
//      case _ =>
//        val nestedFilterStatement = buildConditionForFilter(relationFilter.nestedFilter, newAlias)
//
//        val select = sql
//          .select(relationColumn(relation, relationField.relationSide))
//          .from(modelTable(relationField.relatedModel_!).as(newAlias))
//          .innerJoin(relationTable(relation))
//          .on(modelIdColumn(newAlias, relationField.relatedModel_!).eq(relationColumn(relation, relationField.oppositeRelationSide)))
//
//        val baseField = relField.getOrElse(modelIdColumn(alias, relationField.model))
//
//
//    }
//  }
//
//  private def oneRelationIsNullFilter(relationField: RelationField, alias: String): Condition = {
//    val relation = relationField.relation
//    val select = sql
//      .select(relationColumn(relation, relationField.relationSide))
//      .from(relationTable(relation))
//
//    modelIdColumn(alias, relationField.relatedModel_!).notIn(select)
//  }
}
