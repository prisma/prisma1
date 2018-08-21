package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.gc_values.NullGCValue
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters._

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
      case ScalarFilter(scalarField, Contains(value))            => and(hackForTrue) // fieldFrom(scalarField).contains(stringDummy)
      case ScalarFilter(scalarField, NotContains(value))         => and(hackForTrue) //fieldFrom(scalarField).notContains(stringDummy)
      case ScalarFilter(scalarField, StartsWith(value))          => and(hackForTrue) //fieldFrom(scalarField).startsWith(stringDummy)
      case ScalarFilter(scalarField, NotStartsWith(value))       => and(hackForTrue) //fieldFrom(scalarField).startsWith(stringDummy).not()
      case ScalarFilter(scalarField, EndsWith(value))            => and(hackForTrue) //fieldFrom(scalarField).endsWith(stringDummy)
      case ScalarFilter(scalarField, NotEndsWith(value))         => and(hackForTrue) //fieldFrom(scalarField).endsWith(stringDummy).not()
      case ScalarFilter(scalarField, LessThan(value))            => lt(scalarField.name, value.value)
      case ScalarFilter(scalarField, GreaterThan(value))         => gt(scalarField.name, value.value)
      case ScalarFilter(scalarField, LessThanOrEquals(value))    => lte(scalarField.name, value.value)
      case ScalarFilter(scalarField, GreaterThanOrEquals(value)) => gte(scalarField.name, value.value)
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => notEqual(scalarField.name, null)
      case ScalarFilter(scalarField, NotEquals(value))           => notEqual(scalarField.name, value.value)
      case ScalarFilter(scalarField, Equals(NullGCValue))        => equal(scalarField.name, null)
      case ScalarFilter(scalarField, Equals(value))              => equal(scalarField.name, value.value)
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => in(scalarField.name, null)
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => not(in(scalarField.name, null))
      case ScalarFilter(scalarField, In(values))                 => in(scalarField.name, values.map(_.value): _*)
      case ScalarFilter(scalarField, NotIn(values))              => not(in(scalarField.name, values.map(_.value): _*))
      case OneRelationIsNullFilter(field)                        => and(hackForTrue) //oneRelationIsNullFilter(field, alias)
      case x                                                     => sys.error(s"Not supported: $x")
    }
  }

  val hackForTrue = notEqual("_id", -1)
//  private def relationFilterStatement(alias: String, relationFilter: RelationFilter, relField: Option[Field[AnyRef]], invert: Boolean): Condition = {
//    // this skips intermediate tables when there is no condition on them. so the following will not join with the album table but join the artist-album relation with the album-track relation
//    // artists(where:{albums_some:{tracks_some:{condition}}})
//    //
//    // the following query contains an implicit andFilter around the two nested ones and will not be improved at the moment
//    // albums(where: {Tracks_some:{ MediaType:{Name_starts_with:""}, Genre:{Name_starts_with:""}}})
//    // the same is true for explicit AND, OR, NOT with more than one nested relationfilter. they do not profit from skipping intermediate tables at the moment
//    // these cases could be improved as well at the price of higher code complexity
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
//        (relationFilter.condition, invert) match {
//          case (AtLeastOneRelatedNode, true)  => baseField.notIn(select.and(nestedFilterStatement))
//          case (AtLeastOneRelatedNode, false) => baseField.in(select.and(nestedFilterStatement))
//          case (EveryRelatedNode, true)       => baseField.in(select.andNot(nestedFilterStatement))
//          case (EveryRelatedNode, false)      => baseField.notIn(select.andNot(nestedFilterStatement))
//          case (NoRelatedNode, true)          => baseField.in(select.and(nestedFilterStatement))
//          case (NoRelatedNode, false)         => baseField.notIn(select.and(nestedFilterStatement))
//          case (NoRelationCondition, true)    => baseField.notIn(select.and(nestedFilterStatement))
//          case (NoRelationCondition, false)   => baseField.in(select.and(nestedFilterStatement))
//        }
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
