package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.shared.models.Model
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.conversions

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

    //--------------------------------- First Draft -----------------------------------------------------
    // this joins first and then filters afterwards
    //------------------------------------- First Pass - Join Relations ---------------------------------
    val joins = buildJoinStagesForFilter(queryArguments.filter)

    //------------------------------------- Second Pass - Generate Filter -------------------------------
    val mongoFilter = buildConditionForFilter(queryArguments.filter)
    val filterStage = Seq(`match`(mongoFilter))

    //--------------------------------- Second Draft ----------------------------------------------------
    // this will try to pull match stages before lookup stages where possible

    val joinAndFilter = buildJoinStagesForFilter2(queryArguments.filter)

    //-------------------------------- Order ------------------------------------------------------------
    val sort = Seq(OrderByClauseBuilder.sortStage(queryArguments))

    //-------------------------------- Skip & Limit -----------------------------------------------------
    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)
    val skipStage    = Seq(skip(skipAndLimit.skip))

    val limitStage = skipAndLimit.limit.map(limit)

    //-------------------------------- Project Result ---------------------------------------------------

    //get rid of what was joined
    //apply selected fields

    //--------------------------- Setup Query -----------------------------------------------------------
    val pipeline = cursorMatch ++ joins ++ filterStage ++ sort ++ skipStage ++ limitStage
//    val pipeline = cursorMatch ++ joins

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

  //reorder join filter

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
  //this needs to output pipeline stages for every single filter element in the correct order since Mongo does not pull match before lookup

  def buildJoinStagesForFilter2(filter: Option[Filter]): Seq[conversions.Bson] = filter match {
    case Some(filter) => buildJoinStagesForFilter2(Path.empty, filter)
    case None         => Seq.empty
  }

  private def buildJoinStagesForFilter2(path: Path, filter: Filter): Seq[conversions.Bson] = {
    filter match {
      //-------------------------------RECURSION------------------------------------
      case NodeSubscriptionFilter => Seq.empty
      case AndFilter(filters)     => filters.flatMap(f => buildJoinStagesForFilter2(path, f))
      case OrFilter(filters)      => filters.flatMap(f => buildJoinStagesForFilter2(path, f))
      case NotFilter(filters)     => filters.flatMap(f => buildJoinStagesForFilter2(path, f))
      case NodeFilter(filters)    => filters.flatMap(f => buildJoinStagesForFilter2(path, f))
      case x: RelationFilter      => relationFilterJoinStage2(path, x)

      //--------------------------------ANCHORS------------------------------------
      case TrueFilter                     => Seq.empty
      case FalseFilter                    => Seq.empty
      case ScalarFilter(_, _)             => Seq.empty
      case OneRelationIsNullFilter(field) => Seq.empty // FIXME: Think about this
      case x                              => sys.error(s"Not supported: $x")
    }
  }

  private def relationFilterJoinStage2(path: Path, relationFilter: RelationFilter): Seq[conversions.Bson] = {
    //FIXME: this could also return the projections to clean the result

    val rf          = relationFilter.field
    val updatedPath = path.append(rf)
    val next        = buildJoinStagesForFilter2(updatedPath, relationFilter.nestedFilter)

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

}
