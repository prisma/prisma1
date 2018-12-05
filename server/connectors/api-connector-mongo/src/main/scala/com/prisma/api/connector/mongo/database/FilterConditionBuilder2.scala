package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.HackforTrue.hackForTrue
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.{Model, RelationField, ScalarField}
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters._

import scala.collection.immutable
import scala.concurrent.Future
trait FilterConditionBuilder2 extends FilterConditionBuilder {
  import org.mongodb.scala.bson.collection.immutable.Document
  import org.mongodb.scala.bson.conversions.Bson
  import org.mongodb.scala.model.Aggregates._

  def aggregationQuery(database: MongoDatabase, model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields): Future[Seq[Document]] = {

    //--------------------------- Assemble Pipeline -----------------------------------------------------

    //-------------------------------- Match on Cursor Condition ----------------------------------------
    val cursorMatch: Option[Bson] = CursorConditionBuilder.buildCursorCondition(queryArguments) match {
      case None         => None
      case Some(filter) => Some(filter)
    }

    //-------------------------------- QueryArg Filter --------------------------------------------------

    val joinAndFilter = buildJoinStagesForFilter(queryArguments.filter)

    //get rid of what was joined and remove duplicates
    val scalars: immutable.Seq[(String, Document)] = model.scalarFields.map(f => f.dbName -> Document("$first" -> s"$$${f.dbName}"))
    val doc: Document                              = Document(scalars).+("_id" -> "$_id")
    val mongoGroup                                 = Seq(Document("$group" -> doc))

    //-------------------------------- Project Result ---------------------------------------------------

    //apply selected fields

    //-------------------------------- Order ------------------------------------------------------------
    val sort = Seq(OrderByClauseBuilder.sortStage(queryArguments))

    //-------------------------------- Skip & Limit -----------------------------------------------------
    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)
    val skipStage    = Seq(skip(skipAndLimit.skip))
    val limitStage   = skipAndLimit.limit.map(limit)

    //--------------------------- Setup Query -----------------------------------------------------------
    val pipeline = cursorMatch ++ joinAndFilter ++ mongoGroup ++ sort ++ skipStage ++ limitStage

    database.getCollection(model.dbName).aggregate(pipeline.toSeq).toFuture()
  }

  //-------------------------------------- Join And Filter ---------------------------------------------------
  //this splits up the matches and pulls them before the $lookup

  def buildJoinStagesForFilter(filter: Option[Filter]): Seq[conversions.Bson] = filter match {
    case Some(filter) => buildJoinStagesForFilter2(Path.empty, filter)
    case None         => Seq.empty
  }

  private def buildJoinStagesForFilter2(path: Path, filter: Filter): Seq[conversions.Bson] = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => Seq(`match`(hackForTrue))
      case AndFilter(filters)     => sortFilters(filters).flatMap(f => buildJoinStagesForFilter2(path, f)) // and(nonEmptyConditions(path, filters): _*)
      case OrFilter(filters)      => sys.error("Later") // or(nonEmptyConditions(path, filters): _*)
      case NotFilter(filters) =>
        sys.error("Later") // nor(filters.map(f => buildConditionForFilter(path, f)): _*) //not can only negate equality comparisons not filters
      case NodeFilter(filters) => sys.error("Later") // buildConditionForFilter(path, OrFilter(filters))
      case x: RelationFilter   => relationFilterJoinStage2(path, x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                                            => Seq(`match`(hackForTrue))
      case FalseFilter                                           => Seq(`match`(not(hackForTrue)))
      case ScalarFilter(scalarField, Contains(v))                => Seq(`match`(regex(nameHelper(path, scalarField), v.value.toString)))
      case ScalarFilter(scalarField, NotContains(v))             => Seq(`match`(not(regex(nameHelper(path, scalarField), v.value.toString))))
      case ScalarFilter(scalarField, StartsWith(v))              => Seq(`match`(regex(nameHelper(path, scalarField), "^" + v.value)))
      case ScalarFilter(scalarField, NotStartsWith(v))           => Seq(`match`(not(regex(nameHelper(path, scalarField), "^" + v.value))))
      case ScalarFilter(scalarField, EndsWith(value))            => Seq(`match`(regex(nameHelper(path, scalarField), value.value + "$")))
      case ScalarFilter(scalarField, NotEndsWith(v))             => Seq(`match`(not(regex(nameHelper(path, scalarField), v.value + "$"))))
      case ScalarFilter(scalarField, LessThan(v))                => Seq(`match`(lt(nameHelper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, GreaterThan(v))             => Seq(`match`(gt(nameHelper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, LessThanOrEquals(v))        => Seq(`match`(lte(nameHelper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, GreaterThanOrEquals(v))     => Seq(`match`(gte(nameHelper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => Seq(`match`(notEqual(nameHelper(path, scalarField), null)))
      case ScalarFilter(scalarField, NotEquals(v))               => Seq(`match`(notEqual(nameHelper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, Equals(NullGCValue))        => Seq(`match`(equal(nameHelper(path, scalarField), null)))
      case ScalarFilter(scalarField, Equals(v))                  => Seq(`match`(equal(nameHelper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => Seq(`match`(in(nameHelper(path, scalarField), null)))
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => Seq(`match`(not(in(nameHelper(path, scalarField), null))))
      case ScalarFilter(scalarField, In(values))                 => Seq(`match`(in(nameHelper(path, scalarField), values.map(GCToBson(_)): _*)))
      case ScalarFilter(scalarField, NotIn(values))              => Seq(`match`(not(in(nameHelper(path, scalarField), values.map(GCToBson(_)): _*))))
      case OneRelationIsNullFilter(field)                        => oneRelationNull(field, path)
      //Fixme test this thoroughly
      case ScalarListFilter(scalarListField, ListContains(value)) =>
        Seq(`match`(all(nameHelper(path, scalarListField), GCToBson(value))))
      case ScalarListFilter(scalarListField, ListContainsEvery(values)) =>
        Seq(`match`(all(nameHelper(path, scalarListField), values.map(GCToBson(_)): _*)))
      case ScalarListFilter(scalarListField, ListContainsSome(values)) =>
        Seq(`match`(or(values.map(value => all(nameHelper(path, scalarListField), GCToBson(value))): _*)))
      case x => sys.error(s"Not supported: $x")
    }
  }

  private def oneRelationNull(field: RelationField, path: Path) = {
    //this needs to use the correct fieldName and needs to look on the correct side equal: null should work here

    Seq(`match`(equal(combineTwo(path.combinedNames, field.name), null)))
  }

  private def relationFilterJoinStage2(path: Path, relationFilter: RelationFilter): Seq[conversions.Bson] = {

    val rf          = relationFilter.field
    val updatedPath = path.append(rf)

    val next = buildJoinStagesForFilter2(updatedPath, relationFilter.nestedFilter)

    val current = rf.relatedModel_!.isEmbedded match {
      case true =>
        Seq.empty

      case false =>
        val mongoLookup = rf.relationIsInlinedInParent match {
          case true =>
            lookup(
              localField = combineTwo(path.combinedNames, rf.dbName),
              from = rf.relatedModel_!.dbName,
              foreignField = renameId(rf.relatedModel_!.idField_!),
              as = updatedPath.combinedNames
            )
          case false =>
            lookup(
              localField = combineTwo(path.combinedNames, renameId(rf.model.idField_!)),
              from = rf.relatedModel_!.dbName,
              foreignField = rf.relatedField.dbName,
              as = updatedPath.combinedNames
            )
        }

        val mongoUnwind = unwind(s"$$${updatedPath.combinedNames}")

        Seq(mongoLookup, mongoUnwind)
    }

    current ++ next
  }

  //-------------------------------------------------- Helpers ------------------------------------------------------

  private def nameHelper(path: Path, scalarField: ScalarField): String = combineTwo(path.combinedNames, renameId(scalarField))

  //-------------------------------Determine if Aggregation is needed -----------------------------------

  def needsAggregation(filter: Option[Filter]): Boolean = filter match {
    case Some(filter) => needsAggregation(filter)
    case None         => false
  }

  private def needsAggregation(filter: Filter): Boolean = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => false
      case AndFilter(filters)     => filters.exists(f => needsAggregation(f))
      case OrFilter(filters)      => filters.exists(f => needsAggregation(f))
      case NotFilter(filters)     => filters.exists(f => needsAggregation(f))
      case NodeFilter(filters)    => filters.exists(f => needsAggregation(f))
      case x: RelationFilter      => relationNeedsFilter(x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                 => false
      case FalseFilter                => false
      case ScalarFilter(_, _)         => false
      case ScalarListFilter(_, _)     => false
      case OneRelationIsNullFilter(_) => true
      case x                          => sys.error(s"Not supported: $x")
    }
  }

  private def relationNeedsFilter(relationFilter: RelationFilter): Boolean = {
    val rf      = relationFilter.field
    val next    = needsAggregation(relationFilter.nestedFilter)
    val current = if (rf.relatedModel_!.isEmbedded) false else true

    current || next
  }

  //------------------------------Sort Filters - JoinRelationals Last --------------------------------
  private def sortFilters(filters: Seq[Filter]): Seq[Filter] = {
    val withRelationFilter    = filters.collect { case x if needsAggregation(Some(x))  => x }
    val withoutRelationFilter = filters.collect { case x if !needsAggregation(Some(x)) => x }

    withoutRelationFilter ++ withRelationFilter
  }
}
