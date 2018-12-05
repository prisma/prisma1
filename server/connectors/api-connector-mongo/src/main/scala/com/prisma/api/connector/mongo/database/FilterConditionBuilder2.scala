package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.HackforTrue.hackForTrue
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.{Field, Model, ScalarField}
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.UnwindOptions

import scala.collection.immutable
trait FilterConditionBuilder2 extends FilterConditionBuilder {
  import org.mongodb.scala.bson.collection.immutable.Document
  import org.mongodb.scala.bson.conversions.Bson
  import org.mongodb.scala.model.Aggregates._

  def aggregationQuery(database: MongoDatabase, model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields) = {

    //--------------------------- Assemble Pipeline -----------------------------------------------------

    //-------------------------------- Match on Cursor Condition ----------------------------------------
    val cursorMatch: Option[Bson] = CursorConditionBuilder.buildCursorCondition(queryArguments) match {
      case None         => None
      case Some(filter) => Some(filter)
    }

    //-------------------------------- QueryArg Filter --------------------------------------------------

//    //--------------------------------- First Draft -----------------------------------------------------
//    // this joins first and then filters afterwards
//    //------------------------------------- First Pass - Join Relations ---------------------------------
//    val joins = buildJoinStagesForFilter(queryArguments.filter)
//
//    //------------------------------------- Second Pass - Generate Filter -------------------------------
//    val mongoFilter = buildConditionForFilter(queryArguments.filter)
//    val filterStage = Seq(`match`(mongoFilter))

    //--------------------------------- Second Draft ----------------------------------------------------
    // this will try to pull match stages before lookup stages where possible

    val joinAndFilter = buildJoinStagesForFilter2(queryArguments.filter)

    //-------------------------------- Project Result ---------------------------------------------------

    //get rid of what was joined
    //apply selected fields
    //remove duplicates

    lazy val scalars: immutable.Seq[(String, Document)] = model.scalarFields.map(f => f.dbName -> Document("$first" -> s"$$${f.dbName}"))
    lazy val doc: Document                              = Document(scalars).+("_id" -> "$_id")
    lazy val mongoGroup                                 = Seq(Document("$group" -> doc))

    //-------------------------------- Order ------------------------------------------------------------
    val sort = Seq(OrderByClauseBuilder.sortStage(queryArguments))

    //-------------------------------- Skip & Limit -----------------------------------------------------
    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)
    val skipStage    = Seq(skip(skipAndLimit.skip))

    val limitStage = skipAndLimit.limit.map(limit)

    //--------------------------- Setup Query -----------------------------------------------------------
//    val pipeline = cursorMatch ++ joins ++ filterStage ++ sort ++ skipStage ++ limitStage
//    val pipeline = cursorMatch ++ joins
    val pipeline = cursorMatch ++ joinAndFilter ++ mongoGroup ++ sort ++ skipStage ++ limitStage

    database.getCollection(model.dbName).aggregate(pipeline.toSeq).toFuture()
  }

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
      case TrueFilter                     => false
      case FalseFilter                    => false
      case ScalarFilter(_, _)             => false
      case OneRelationIsNullFilter(field) => false // FIXME: Think about this
      case x                              => sys.error(s"Not supported: $x")
    }
  }

  private def relationNeedsFilter(relationFilter: RelationFilter): Boolean = {
    val rf      = relationFilter.field
    val next    = needsAggregation(relationFilter.nestedFilter)
    val current = if (rf.relatedModel_!.isEmbedded) false else true

    current || next
  }

  //-------------------------------------- Join Stage 1 ---------------------------------------------------

  def buildJoinStagesForFilter(filter: Option[Filter]): Seq[conversions.Bson] = filter match {
    case Some(filter) => buildJoinStagesForFilter(Path.empty, filter)
    case None         => Seq.empty
  }

  private def buildJoinStagesForFilter(path: Path, filter: Filter): Seq[conversions.Bson] = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => Seq.empty
      case AndFilter(filters)     => filters.flatMap(f => buildJoinStagesForFilter(path, f))
      case OrFilter(filters)      => filters.flatMap(f => buildJoinStagesForFilter(path, f))
      case NotFilter(filters)     => filters.flatMap(f => buildJoinStagesForFilter(path, f))
      case NodeFilter(filters)    => filters.flatMap(f => buildJoinStagesForFilter(path, f))
      case x: RelationFilter      => relationFilterJoinStage(path, x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                     => Seq.empty
      case FalseFilter                    => Seq.empty
      case ScalarFilter(_, _)             => Seq.empty
      case OneRelationIsNullFilter(field) => Seq.empty // FIXME: Think about this
      case x                              => sys.error(s"Not supported: $x")
    }
  }

  private def relationFilterJoinStage(path: Path, relationFilter: RelationFilter): Seq[conversions.Bson] = {
    //FIXME: this could also return the projections to clean the result

    val rf          = relationFilter.field
    val updatedPath = path.append(rf)
    val next        = buildJoinStagesForFilter(updatedPath, relationFilter.nestedFilter)

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

        //group needs to render all fields in here (at least all the ones referred to by the filters)
        lazy val previousModel                              = path.segments.last.rf.model
        lazy val scalars: immutable.Seq[(String, Document)] = previousModel.scalarFields.map(f => f.dbName -> Document("$first" -> s"$$${f.dbName}"))
        lazy val added: Seq[(String, Document)]             = Seq((s"${path.combinedNames}", Document("$push" -> s"$$${path.combinedNames}")))
        lazy val doc: Document                              = Document(scalars ++ added).+("_id" -> "$_id")
        lazy val mongoGroup                                 = Document("$group" -> doc)

        (path.segments.isEmpty, rf.isList) match {
          case (true, true) if next.isEmpty  => Seq(mongoLookup)
          case (true, true) if next.nonEmpty => Seq(mongoLookup, mongoUnwind)
          case (true, false)                 => Seq(mongoLookup, mongoUnwind)
          case (false, true)                 => Seq(mongoLookup, mongoGroup)
          case (false, false)                => Seq(mongoLookup, mongoUnwind)
        }
    }

    current ++ next
  }

  //-------------------------------------- Join Stage 2 ---------------------------------------------------
  //this splits up the matches and pulls them before the $lookup

  def buildJoinStagesForFilter2(filter: Option[Filter]): Seq[conversions.Bson] = filter match {
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
//      case _ => Seq.empty
      case TrueFilter                                            => Seq(`match`(hackForTrue))
      case FalseFilter                                           => Seq(`match`(not(hackForTrue)))
      case ScalarFilter(scalarField, Contains(v))                => Seq(`match`(regex(helper(path, scalarField), v.value.toString)))
      case ScalarFilter(scalarField, NotContains(v))             => Seq(`match`(not(regex(helper(path, scalarField), v.value.toString))))
      case ScalarFilter(scalarField, StartsWith(v))              => Seq(`match`(regex(helper(path, scalarField), "^" + v.value)))
      case ScalarFilter(scalarField, NotStartsWith(v))           => Seq(`match`(not(regex(helper(path, scalarField), "^" + v.value))))
      case ScalarFilter(scalarField, EndsWith(value))            => Seq(`match`(regex(helper(path, scalarField), value.value + "$")))
      case ScalarFilter(scalarField, NotEndsWith(v))             => Seq(`match`(not(regex(helper(path, scalarField), v.value + "$"))))
      case ScalarFilter(scalarField, LessThan(v))                => Seq(`match`(lt(helper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, GreaterThan(v))             => Seq(`match`(gt(helper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, LessThanOrEquals(v))        => Seq(`match`(lte(helper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, GreaterThanOrEquals(v))     => Seq(`match`(gte(helper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, NotEquals(NullGCValue))     => Seq(`match`(notEqual(helper(path, scalarField), null)))
      case ScalarFilter(scalarField, NotEquals(v))               => Seq(`match`(notEqual(helper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, Equals(NullGCValue))        => Seq(`match`(equal(helper(path, scalarField), null)))
      case ScalarFilter(scalarField, Equals(v))                  => Seq(`match`(equal(helper(path, scalarField), GCToBson(v))))
      case ScalarFilter(scalarField, In(Vector(NullGCValue)))    => Seq(`match`(in(helper(path, scalarField), null)))
      case ScalarFilter(scalarField, NotIn(Vector(NullGCValue))) => Seq(`match`(not(in(helper(path, scalarField), null))))
      case ScalarFilter(scalarField, In(values))                 => Seq(`match`(in(helper(path, scalarField), values.map(GCToBson(_)): _*)))
      case ScalarFilter(scalarField, NotIn(values))              => Seq(`match`(not(in(helper(path, scalarField), values.map(GCToBson(_)): _*))))
      case OneRelationIsNullFilter(field)                        => Seq(`match`(equal(combineTwo(path.combinedNames, field.name), null)))
      //Fixme test this thoroughly
      case ScalarListFilter(scalarListField, ListContains(value)) =>
        Seq(`match`(all(helper(path, scalarListField), GCToBson(value))))
      case ScalarListFilter(scalarListField, ListContainsEvery(values)) =>
        Seq(`match`(all(helper(path, scalarListField), values.map(GCToBson(_)): _*)))
      case ScalarListFilter(scalarListField, ListContainsSome(values)) =>
        Seq(`match`(or(values.map(value => all(helper(path, scalarListField), GCToBson(value))): _*)))
      case x => sys.error(s"Not supported: $x")
    }
  }

  private def sortFilters(filters: Seq[Filter]): Seq[Filter] = {
    val withRelationFilter    = filters.collect { case x if needsAggregation(Some(x))  => x }
    val withoutRelationFilter = filters.collect { case x if !needsAggregation(Some(x)) => x }

    withoutRelationFilter ++ withRelationFilter
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

    current ++ next // ++ cleanup projections??
  }

  //-------------------------------------------------- Helpers ------------------------------------------------------

  def helper(path: Path, scalarField: ScalarField) = combineTwo(path.combinedNames, renameId(scalarField))

}
