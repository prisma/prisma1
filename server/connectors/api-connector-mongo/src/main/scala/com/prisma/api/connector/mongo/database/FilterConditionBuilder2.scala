package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.DocumentToRoot
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.HackforTrue.hackForTrue
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.{Model, ScalarField}
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{FindObservable, MongoDatabase}

//relationfilters depend on relationtype
// embedded -> use dot notation to go deeper in tree
// nonEmbedded -> not supported, Inputtypes for filter should not be generated in api
//field_every:  $not $elemMatch ($not nested)
//field_some:   $elemMatch (nested)
//field_none:   $not $elemMatch (nested)

trait FilterConditionBuilder2 {
  import org.mongodb.scala.bson.collection.immutable.Document
  import org.mongodb.scala.bson.conversions.Bson
  import org.mongodb.scala.model.Aggregates._
  import org.mongodb.scala.model.Projections._
  import scala.concurrent.ExecutionContext.Implicits.global

  def aggregationQuery(database: MongoDatabase, model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields) = {

    //--------------------------- Assemble Pipeline -----------------------------------------------------

    //-------------------------------- Match on Cursor Condition ----------------------------------------
    val cursorMatch: Option[Bson] = CursorConditionBuilder.buildCursorCondition(queryArguments) match {
      case None         => None
      case Some(filter) => Some(filter)
    }

    //-------------------------------- QueryArg Filter --------------------------------------------------

    //------------------------------------- First Pass - Join Relations ---------------------------------
    val joins = buildJoinStagesForFilter(queryArguments.filter)

    //------------------------------------- Second Pass - Generate Filter -------------------------------
    val mongoFilter = buildConditionForFilter2(queryArguments.filter)
    val filterStage = Seq(`match`(mongoFilter))

    //-------------------------------- Order ------------------------------------------------------------
    val sort = Seq(OrderByClauseBuilder.sortStage(queryArguments))

    //-------------------------------- Skip & Limit -----------------------------------------------------
    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)
    val skipStage    = Seq(skip(skipAndLimit.skip))

    val limitStage = skipAndLimit.limit.map(limit)

    //-------------------------------- Project Result ---------------------------------------------------

    //--------------------------- Setup Query -----------------------------------------------------------
    val pipeline3 = cursorMatch ++ joins ++ filterStage ++ sort ++ skipStage ++ limitStage
//    val pipeline3 = cursorMatch ++ joins
    val query3 = database.getCollection(model.dbName).aggregate(pipeline3.toSeq)

    //--------------------------- Parse Result ----------------------------------------------------------

    val nodes = query3.collect().toFuture.map { results: Seq[Document] =>
      results.map { result =>
        val root = DocumentToRoot(model, result)
        PrismaNode(root.idField, root, Some(model.name))
      }
    }

    nodes.map(n => ResolverResult[PrismaNode](queryArguments, n.toVector))
  }

  //-------------------------------------- Join Stage ----------------------------------------------------------------------------------------------------------

  def buildJoinStagesForFilter(filter: Option[Filter]): Seq[conversions.Bson] = filter match {
    case Some(filter) => buildJoinStagesForFilter2("", filter)
    case None         => Seq.empty
  }

  private def buildJoinStagesForFilter2(path: String, filter: Filter): Seq[conversions.Bson] = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => Seq.empty
      case AndFilter(filters)     => filters.flatMap(f => buildJoinStagesForFilter2(path, f))
      case OrFilter(filters)      => filters.flatMap(f => buildJoinStagesForFilter2(path, f))
      case NotFilter(filters)     => filters.flatMap(f => buildJoinStagesForFilter2(path, f)) //not can only negate equality comparisons not filters
      case NodeFilter(filters)    => filters.flatMap(f => buildJoinStagesForFilter2(path, f))
      case x: RelationFilter      => relationFilterJoinStage(path, x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                     => Seq.empty
      case FalseFilter                    => Seq.empty
      case ScalarFilter(_, _)             => Seq.empty
      case OneRelationIsNullFilter(field) => Seq.empty // FIXME: Think about this
      case x                              => sys.error(s"Not supported: $x")
    }
  }

  private def relationFilterJoinStage(path: String, relationFilter: RelationFilter): Seq[conversions.Bson] = {
    val rf = relationFilter.field

    val current = rf.relatedModel_!.isEmbedded match {
      case true => Seq.empty
      case false =>
        val mongoLookup = lookup(localField = rf.dbName, from = rf.relatedModel_!.dbName, foreignField = renameId(rf.relatedModel_!.idField_!), as = rf.name)

        rf.isList match {
          case true  => Seq(mongoLookup)
          case false => Seq(mongoLookup, unwind(s"$$${rf.name}")) //add unwind here
        }
    }

    val next = buildJoinStagesForFilter2(combineTwo(path, relationFilter.field.name), relationFilter.nestedFilter)

    current ++ next
  }

  //----------------------------------------- Normal Filters ---------------------------------------------------------------------------------------------------

  def buildConditionForFilter2(filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter2("", filter)
    case None         => Document()
  }

  def buildConditionForScalarFilter2(operator: String, filter: Option[Filter]): conversions.Bson = filter match {
    case Some(filter) => buildConditionForFilter2(operator, filter)
    case None         => Document()
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
    lazy val toOneNested  = buildConditionForFilter2(combineTwo(path, relationFilter.field.name), relationFilter.nestedFilter)
    lazy val toManyNested = buildConditionForFilter2("", relationFilter.nestedFilter)

    relationFilter.condition match {
      case AtLeastOneRelatedNode => elemMatch(relationFilter.field.name, toManyNested)
      case EveryRelatedNode      => not(elemMatch(relationFilter.field.name, not(toManyNested)))
      case NoRelatedNode         => not(elemMatch(relationFilter.field.name, toManyNested))
      case ToOneRelatedNode      => toOneNested
    }
  }
}
