package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.DocumentToRoot
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.HackforTrue.hackForTrue
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.{Model, ScalarField}
import org.mongodb.scala.{Document, FindObservable, MongoDatabase}
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters._

//relationfilters depend on relationtype
// embedded -> use dot notation to go deeper in tree
// nonEmbedded -> not supported, Inputtypes for filter should not be generated in api
//field_every:  $not $elemMatch ($not nested)
//field_some:   $elemMatch (nested)
//field_none:   $not $elemMatch (nested)

trait FilterConditionBuilder2 {
  import scala.concurrent.ExecutionContext.Implicits.global
  def aggregationQuery(database: MongoDatabase, model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields) = {
    import org.mongodb.scala.bson.collection.immutable.Document
    import org.mongodb.scala.bson.conversions.Bson
    import org.mongodb.scala.model.Accumulators._
    import org.mongodb.scala.model.Aggregates._
    import org.mongodb.scala.model.Projections._
    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)

    val mongoFilter = buildConditionForFilter2(queryArguments.filter)

    val combinedFilter = CursorConditionBuilder.buildCursorCondition(queryArguments) match {
      case None         => mongoFilter
      case Some(filter) => Filters.and(mongoFilter, filter)
    }

    val baseQuery: FindObservable[Document]      = database.getCollection(model.dbName).find(combinedFilter)
    val queryWithOrder: FindObservable[Document] = OrderByClauseBuilder.queryWithOrder(baseQuery, queryArguments)
    val queryWithSkip: FindObservable[Document]  = queryWithOrder.skip(skipAndLimit.skip)

    val queryWithLimit = skipAndLimit.limit match {
      case Some(limit) => queryWithSkip.limit(limit)
      case None        => queryWithSkip
    }

    //--------------------------------------------------------------------------------------------------
    val rf = model.relationFields.head
    val f  = rf.relatedModel_!.getScalarFieldByName_!("name")

    val mongoLookup: Bson = lookup(localField = rf.dbName, from = rf.relatedModel_!.dbName, foreignField = renameId(rf.relatedModel_!.idField_!), as = "test")
    val mongoUnwind: Bson = unwind("$test")
//    val mongoMatch: Bson      = `match`(elemMatch("test", equal("name_column", "blog 1")))
    val mongoMatch: Bson      = `match`(equal("test.name_column", "blog 1"))
    val mongoProjection: Bson = project(Document("test" -> 0))

    val pipeline: Seq[conversions.Bson] = Seq(mongoLookup, mongoUnwind, mongoMatch, mongoProjection)

    val query2 = database.getCollection(model.dbName).aggregate(pipeline)

    val nodes = query2.collect().toFuture.map { results: Seq[Document] =>
      results.map { result =>
        val root = DocumentToRoot(model, result)
        PrismaNode(root.idField, root, Some(model.name))
      }
    }

    nodes.map(n => ResolverResult[PrismaNode](queryArguments, n.toVector))
  }

  def buildConditionForFilter2(filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter2("", filter)
    case None         => hackForTrue
  }

  def buildConditionForScalarFilter2(operator: String, filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter2(operator, filter)
    case None         => hackForTrue
  }

  private def buildConditionForFilter2(path: String, filter: Filter): conversions.Bson = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => hackForTrue
      case AndFilter(filters)     => and(nonEmptyConditions(path, filters): _*)
      case OrFilter(filters)      => or(nonEmptyConditions(path, filters): _*)
      case NotFilter(filters)     => nor(filters.map(f => buildConditionForFilter2(path, f)): _*) //not can only negate equality comparisons not filters
      case NodeFilter(filters)    => buildConditionForFilter2(path, OrFilter(filters))
      case x: RelationFilter      => relationFilterStatement(path, x)

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
  }

  private def renameId(field: ScalarField): String = if (field.isId) "_id" else field.dbName

  private def nonEmptyConditions(path: String, filters: Vector[Filter]): Vector[conversions.Bson] = filters.map(f => buildConditionForFilter2(path, f)) match {
    case x if x.isEmpty && path == "" => Vector(hackForTrue)
    case x if x.isEmpty               => Vector(notEqual(s"$path._id", -1))
    case x                            => x
  }

  private def relationFilterStatement(path: String, relationFilter: RelationFilter) = {
    val toOneNested  = buildConditionForFilter2(combineTwo(path, relationFilter.field.name), relationFilter.nestedFilter)
    val toManyNested = buildConditionForFilter2("", relationFilter.nestedFilter)

    relationFilter.condition match {
      case AtLeastOneRelatedNode => elemMatch(relationFilter.field.name, toManyNested)
      case EveryRelatedNode      => not(elemMatch(relationFilter.field.name, not(toManyNested)))
      case NoRelatedNode         => not(elemMatch(relationFilter.field.name, toManyNested))
      case ToOneRelatedNode      => toOneNested
    }
  }
}
