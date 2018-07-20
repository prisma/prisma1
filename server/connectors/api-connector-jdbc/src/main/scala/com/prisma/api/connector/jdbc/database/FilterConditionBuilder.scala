package com.prisma.api.connector.jdbc.database

import com.prisma.api.connector._
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.{RelationField, ScalarField}
import org.jooq.{Condition, Field}
import org.jooq.impl.DSL._

trait FilterConditionBuilder extends BuilderBase {
  def buildConditionForFilter(filter: Option[Filter]): Condition = filter match {
    case Some(filter) => buildConditionForFilter(filter, topLevelAlias)
    case None         => noCondition()
  }

  private def buildConditionForFilter(filter2: Filter, alias: String, relField: Option[Field[AnyRef]] = None, invert: Boolean = false): Condition = {

    def stripLogicalFiltersWithOnlyOneFilterContained(filter: Filter): Filter = {
      filter match {
        case NodeSubscriptionFilter()        => NodeSubscriptionFilter()
        case AndFilter(Vector(singleFilter)) => stripLogicalFiltersWithOnlyOneFilterContained(singleFilter)
        case OrFilter(Vector(singleFilter))  => stripLogicalFiltersWithOnlyOneFilterContained(singleFilter)
        case AndFilter(filters)              => AndFilter(filters.map(stripLogicalFiltersWithOnlyOneFilterContained))
        case OrFilter(filters)               => OrFilter(filters.map(stripLogicalFiltersWithOnlyOneFilterContained))
        case NotFilter(filters)              => NotFilter(filters.map(stripLogicalFiltersWithOnlyOneFilterContained))
        case NodeFilter(filters)             => NodeFilter(filters.map(stripLogicalFiltersWithOnlyOneFilterContained))
        case x: RelationFilter               => x.copy(nestedFilter = stripLogicalFiltersWithOnlyOneFilterContained(x.nestedFilter))
        case x                               => x
      }
    }

    val filter = stripLogicalFiltersWithOnlyOneFilterContained(filter2)

    def fieldFrom(scalarField: ScalarField) = field(name(alias, scalarField.dbName))
    def nonEmptyConditions(filters: Vector[Filter]): Vector[Condition] = {
      filters.map(buildConditionForFilter(_, alias)) match {
        case x if x.isEmpty => Vector(noCondition())
        case x              => x
      }
    }

    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter() => and(trueCondition())
      case AndFilter(filters)       => nonEmptyConditions(filters).reduceLeft(_ and _)
      case OrFilter(filters)        => nonEmptyConditions(filters).reduceLeft(_ or _)
      case NotFilter(filters)       => filters.map(buildConditionForFilter(_, alias)).foldLeft(and(trueCondition()))(_ andNot _)
      case NodeFilter(filters)      => buildConditionForFilter(OrFilter(filters), alias)
      case x: RelationFilter        => relationFilterStatementWithInQueriesAndSkippingTables(alias, x, relField, invert)
//      case x: RelationFilter => relationFilterStatementWithInQueries(alias, x)
//      case x: RelationFilter => relationFilterStatementUnoptimized(alias, x)
      //--------------------------------ANCHORS------------------------------------
      case PreComputedSubscriptionFilter(value)                  => if (value) trueCondition() else falseCondition()
      case ScalarFilter(scalarField, Contains(_))                => fieldFrom(scalarField).contains(stringDummy)
      case ScalarFilter(scalarField, NotContains(_))             => fieldFrom(scalarField).notContains(stringDummy)
      case ScalarFilter(scalarField, StartsWith(_))              => fieldFrom(scalarField).startsWith(stringDummy)
      case ScalarFilter(scalarField, NotStartsWith(_))           => fieldFrom(scalarField).startsWith(stringDummy).not()
      case ScalarFilter(scalarField, EndsWith(_))                => fieldFrom(scalarField).endsWith(stringDummy)
      case ScalarFilter(scalarField, NotEndsWith(_))             => fieldFrom(scalarField).endsWith(stringDummy).not()
      case ScalarFilter(scalarField, LessThan(_))                => fieldFrom(scalarField).lessThan(stringDummy)
      case ScalarFilter(scalarField, GreaterThan(_))             => fieldFrom(scalarField).greaterThan(stringDummy)
      case ScalarFilter(scalarField, LessThanOrEquals(_))        => fieldFrom(scalarField).lessOrEqual(stringDummy)
      case ScalarFilter(scalarField, GreaterThanOrEquals(_))     => fieldFrom(scalarField).greaterOrEqual(stringDummy)
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => fieldFrom(scalarField).isNotNull
      case ScalarFilter(scalarField, NotEquals(_))               => fieldFrom(scalarField).notEqual(stringDummy)
      case ScalarFilter(scalarField, Equals(NullGCValue))        => fieldFrom(scalarField).isNull
      case ScalarFilter(scalarField, Equals(_))                  => fieldFrom(scalarField).equal(stringDummy)
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => fieldFrom(scalarField).isNull
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => fieldFrom(scalarField).isNotNull
      case ScalarFilter(scalarField, In(values))                 => fieldFrom(scalarField).in(Vector.fill(values.length) { stringDummy }: _*)
      case ScalarFilter(scalarField, NotIn(values))              => fieldFrom(scalarField).notIn(Vector.fill(values.length) { stringDummy }: _*)
      case OneRelationIsNullFilter(field)                        => oneRelationIsNullFilter(field, alias)
      case x                                                     => sys.error(s"Not supported: $x")
    }
  }

  private def relationFilterStatementUnoptimized(alias: String, relationFilter: RelationFilter): Condition = {
    val relationField         = relationFilter.field
    val relation              = relationField.relation
    val newAlias              = relationField.relatedModel_!.dbName + "_" + alias
    val nestedFilterStatement = buildConditionForFilter(relationFilter.nestedFilter, newAlias)

    val select = sql
      .select()
      .from(modelTable(relationField.relatedModel_!).as(newAlias))
      .innerJoin(relationTable(relation))
      .on(modelIdColumn(newAlias, relationField.relatedModel_!).eq(relationColumn(relation, relationField.oppositeRelationSide)))
      .where(relationColumn(relation, relationField.relationSide).eq(modelIdColumn(alias, relationField.model)))

    relationFilter.condition match {
      case AtLeastOneRelatedNode => exists(select.and(nestedFilterStatement))
      case EveryRelatedNode      => notExists(select.andNot(nestedFilterStatement))
      case NoRelatedNode         => notExists(select.and(nestedFilterStatement))
      case NoRelationCondition   => exists(select.and(nestedFilterStatement))
    }
  }

  private def relationFilterStatementWithInQueries(alias: String, relationFilter: RelationFilter): Condition = {
    val relationField         = relationFilter.field
    val relation              = relationField.relation
    val newAlias              = relationField.relatedModel_!.dbName + "_" + alias
    val nestedFilterStatement = buildConditionForFilter(relationFilter.nestedFilter, newAlias)

    val select = sql
      .select(relationColumn(relation, relationField.relationSide))
      .from(modelTable(relationField.relatedModel_!).as(newAlias))
      .innerJoin(relationTable(relation))
      .on(modelIdColumn(newAlias, relationField.relatedModel_!).eq(relationColumn(relation, relationField.oppositeRelationSide)))

    relationFilter.condition match {
      case AtLeastOneRelatedNode => modelIdColumn(alias, relationField.model).in(select.and(nestedFilterStatement))
      case EveryRelatedNode      => modelIdColumn(alias, relationField.model).notIn(select.andNot(nestedFilterStatement))
      case NoRelatedNode         => modelIdColumn(alias, relationField.model).notIn(select.and(nestedFilterStatement))
      case NoRelationCondition   => modelIdColumn(alias, relationField.model).in(select.and(nestedFilterStatement))
    }
  }

  private def relationFilterStatementWithInQueriesAndSkippingTables(alias: String,
                                                                    relationFilter: RelationFilter,
                                                                    relField: Option[Field[AnyRef]],
                                                                    invert: Boolean): Condition = {
    // this skips intermediate table when there is no condition on it
    // artists(where:{albums_some:{tracks_some:{condition}}})
    // albums(where: {Tracks_some:{ MediaType:{Name_starts_with:""}, Genre:{Name_starts_with:""}}})    Todo also on cases where there are several filters below
    // this will be an andFilter around the two nested ones

    val relationField = relationFilter.field
    val relation      = relationField.relation
    val newAlias      = relationField.relatedModel_!.dbName + "_" + alias

    relationFilter.nestedFilter match {
//      case AndFilter(x) =>
//      case OrFilter(x) =>
//      case NotFilter(x) =>
//        // if the andfilter contains only relationFilters shortcut all of them and fold them together with and
//        // if it's a mix we can't shortcut
      // shortcuts for not and or also need to be standardized
      // maybe we do not need the step that gets rid of unnecessary ands/ors? since we need to handle them anyway?
//
//
//        val relField = Some(relationColumn(relation, relationField.oppositeRelationSide))
//        val select = sql
//          .select(relationColumn(relation, relationField.relationSide))
//          .from(relationTable(relation))
//
//        relationFilter.condition match {
//          case AtLeastOneRelatedNode => modelIdColumn(alias, relationField.model).in(select.where(buildConditionForFilter(x, newAlias, relField)))
//          case EveryRelatedNode      => modelIdColumn(alias, relationField.model).notIn(select.where(buildConditionForFilter(x, newAlias, relField, true)))
//          case NoRelatedNode         => modelIdColumn(alias, relationField.model).notIn(select.where(buildConditionForFilter(x, newAlias, relField)))
//          case NoRelationCondition   => modelIdColumn(alias, relationField.model).in(select.where(buildConditionForFilter(x, newAlias, relField)))
//        }

      case x: RelationFilter =>
        val relField = Some(relationColumn(relation, relationField.oppositeRelationSide))
        val select = sql
          .select(relationColumn(relation, relationField.relationSide))
          .from(relationTable(relation))

        relationFilter.condition match {
          case AtLeastOneRelatedNode => modelIdColumn(alias, relationField.model).in(select.where(buildConditionForFilter(x, newAlias, relField)))
          case EveryRelatedNode      => modelIdColumn(alias, relationField.model).notIn(select.where(buildConditionForFilter(x, newAlias, relField, true)))
          case NoRelatedNode         => modelIdColumn(alias, relationField.model).notIn(select.where(buildConditionForFilter(x, newAlias, relField)))
          case NoRelationCondition   => modelIdColumn(alias, relationField.model).in(select.where(buildConditionForFilter(x, newAlias, relField)))
        }

      case _ =>
        val nestedFilterStatement = buildConditionForFilter(relationFilter.nestedFilter, newAlias)

        val select = sql
          .select(relationColumn(relation, relationField.relationSide))
          .from(modelTable(relationField.relatedModel_!).as(newAlias))
          .innerJoin(relationTable(relation))
          .on(modelIdColumn(newAlias, relationField.relatedModel_!).eq(relationColumn(relation, relationField.oppositeRelationSide)))

        val baseField = relField.getOrElse(modelIdColumn(alias, relationField.model))

        (relationFilter.condition, invert) match {
          case (AtLeastOneRelatedNode, true)  => baseField.notIn(select.and(nestedFilterStatement))
          case (AtLeastOneRelatedNode, false) => baseField.in(select.and(nestedFilterStatement))
          case (EveryRelatedNode, true)       => baseField.in(select.andNot(nestedFilterStatement))
          case (EveryRelatedNode, false)      => baseField.notIn(select.andNot(nestedFilterStatement))
          case (NoRelatedNode, true)          => baseField.in(select.and(nestedFilterStatement))
          case (NoRelatedNode, false)         => baseField.notIn(select.and(nestedFilterStatement))
          case (NoRelationCondition, true)    => baseField.notIn(select.and(nestedFilterStatement))
          case (NoRelationCondition, false)   => baseField.in(select.and(nestedFilterStatement))
        }
    }
  }

  private def oneRelationIsNullFilter(relationField: RelationField, alias: String): Condition = {
    val relation = relationField.relation
    val select = sql
      .select()
      .from(relationTable(relation))
      .where(relationColumn(relation, relationField.relationSide).eq(modelIdColumn(alias, relationField.relatedModel_!)))

    notExists(select)
  }
}
