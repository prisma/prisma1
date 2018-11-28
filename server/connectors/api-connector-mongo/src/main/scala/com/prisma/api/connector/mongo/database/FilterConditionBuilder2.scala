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
trait FilterConditionBuilder2 extends FilterConditionBuilder {
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
    val mongoFilter = buildConditionForFilter(queryArguments.filter)
    val filterStage = Seq(`match`(mongoFilter))

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
    val query = database.getCollection(model.dbName).aggregate(pipeline.toSeq)

    //--------------------------- Parse Result ----------------------------------------------------------

    val nodes = query.collect().toFuture.map { results: Seq[Document] =>
      results.map { result =>
        val root = DocumentToRoot(model, result)
        PrismaNode(root.idField, root, Some(model.name))
      }
    }

    nodes.map(n => ResolverResult[PrismaNode](queryArguments, n.toVector))
  }

  //-------------------------------------- Join Stage ----------------------------------------------------------------------------------------------------------

  def buildJoinStagesForFilter(filter: Option[Filter]): Seq[conversions.Bson] = filter match {
    case Some(filter) => buildJoinStagesForFilter("", filter)
    case None         => Seq.empty
  }

  private def buildJoinStagesForFilter(path: String, filter: Filter): Seq[conversions.Bson] = {
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

  private def relationFilterJoinStage(path: String, relationFilter: RelationFilter): Seq[conversions.Bson] = {
    //FIXME: this could also return the projections to clean the result

    val rf = relationFilter.field

    val current = rf.relatedModel_!.isEmbedded match {
      case true => Seq.empty
      case false =>
        val mongoLookup = rf.relationIsInlinedInParent match {
          case true =>
            lookup(localField = combineTwo(path, rf.dbName),
                   from = rf.relatedModel_!.dbName,
                   foreignField = renameId(rf.relatedModel_!.idField_!),
                   as = combineTwo(path, rf.name))
          case false =>
            lookup(
              localField = combineTwo(path, renameId(rf.model.idField_!)),
              from = rf.relatedModel_!.dbName,
              foreignField = rf.relatedField.dbName,
              as = combineTwo(path, rf.name)
            )
        }
        rf.isList match {
          case true  => Seq(mongoLookup)
          case false => Seq(mongoLookup, unwind(s"$$${combineTwo(path, rf.name)}"))
        }
    }

    val next = buildJoinStagesForFilter(combineTwo(path, relationFilter.field.name), relationFilter.nestedFilter)

    current ++ next
  }
}
