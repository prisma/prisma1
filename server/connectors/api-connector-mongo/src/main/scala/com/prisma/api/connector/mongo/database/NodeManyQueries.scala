package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.MongoResultReader
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.gc_values.{GCValue, IdGCValue, StringIdGCValue}
import com.prisma.shared.models.{Model, RelationField}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, FindObservable, MongoDatabase}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.existentials

trait NodeManyQueries extends FilterConditionBuilder with AggregationQueryBuilder with ProjectionBuilder with MongoResultReader {

  def getNodes(model: Model, queryArguments: QueryArguments, selectedFields: SelectedFields) = SimpleMongoAction { database =>
    manyQueryHelper(model, queryArguments, None, database, false, selectedFields).map { results: Seq[Document] =>
      val nodes = results.map(readsPrismaNode(_, model, selectedFields))
      ResolverResult[PrismaNode](queryArguments, nodes.toVector)
    }
  }

  def getNodeIdsByFilter(model: Model, filter: Option[Filter]): SimpleMongoAction[Seq[IdGCValue]] = SimpleMongoAction { database =>
    if (needsAggregation(filter)) {
      aggregationQueryForId(database, model, QueryArguments.withFilter(filter))
    } else {
      database
        .getCollection(model.dbName)
        .find(buildConditionForFilter(filter))
        .projection(idProjection)
        .collect()
        .toFuture
        .map(_.map(readsId))
    }
  }

  def manyQueryHelper(model: Model,
                      queryArguments: QueryArguments,
                      extraFilter: Option[Filter] = None,
                      database: MongoDatabase,
                      idOnly: Boolean = false,
                      selectedFields: SelectedFields): Future[Seq[Document]] = {

    // run the subquery first for cursor if necessary

    CursorConditionBuilder
      .fetchCursorRowValueById(database, model, queryArguments)
      .map { rowValueOpt =>
        val updatedQueryArgs = extraFilter match {
          case Some(inFilter) => queryArguments.copy(filter = Some(AndFilter(Vector(inFilter) ++ queryArguments.filter)))
          case None           => queryArguments
        }

        if (needsAggregation(updatedQueryArgs.filter)) {
          aggregationQuery(database, model, updatedQueryArgs, selectedFields, rowValueOpt)
        } else {

          val skipAndLimit = LimitClauseHelper.skipAndLimitValues(updatedQueryArgs)
          val mongoFilter  = buildConditionForFilter(updatedQueryArgs.filter)

          val combinedFilter = rowValueOpt match {
            case Some(rowValue) => Filters.and(mongoFilter, CursorConditionBuilder.buildCursorCondition(model, queryArguments, rowValue).get)
            case None           => mongoFilter
          }

          val baseQuery: FindObservable[Document]      = database.getCollection(model.dbName).find(combinedFilter)
          val queryWithOrder: FindObservable[Document] = baseQuery.sort(OrderByClauseBuilder.sortBson(queryArguments))
          val queryWithSkip: FindObservable[Document]  = queryWithOrder.skip(skipAndLimit.skip)

          val queryWithLimit = skipAndLimit.limit match {
            case Some(limit) => queryWithSkip.limit(limit)
            case None        => queryWithSkip
          }

          idOnly match {
            case true  => queryWithLimit.projection(idProjection).collect().toFuture
            case false => queryWithLimit.projection(projectSelected(selectedFields)).collect().toFuture
          }
        }
      }
      .flatten
  }

  def getRelatedNodes(fromField: RelationField, fromNodeIds: Vector[IdGCValue], queryArguments: QueryArguments, selectedFields: SelectedFields) =
    SimpleMongoAction { database =>
      val relatedField = fromField.relatedField
      val model        = fromField.relatedModel_!

      val inFilter: Filter      = ScalarListFilter(model.dummyField(relatedField), ListContainsSome(fromNodeIds))
      val updatedSelectedFields = selectedFields ++ SelectedFields.forRelationField(relatedField)
      manyQueryHelper(model, queryArguments, Some(inFilter), database, false, updatedSelectedFields)
        .map { results: Seq[Document] =>
          val groups: Map[StringIdGCValue, Seq[Document]] = relatedField.isList match {
            case true =>
              val tuples = for {
                result <- results
                id     <- result(relatedField.dbName).asArray().getValues.asScala.map(_.asObjectId()).map(x => StringIdGCValue(x.getValue.toString))
              } yield (id, result)

              tuples.groupBy(_._1).mapValues(_.map(_._2))

            case false =>
              results.groupBy(x => StringIdGCValue(x(relatedField.dbName).asObjectId().getValue.toString))
          }

          fromNodeIds.map { id =>
            groups.get(id.asInstanceOf[StringIdGCValue]) match {
              case Some(group) => ResolverResult(queryArguments, group.map(readsPrismaNodeWithParent(_, model, selectedFields, id)).toVector, Some(id))
              case None        => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id))
            }
          }
        }
    }

  //Fixme this does not use all queryarguments
  def countFromModel(model: Model, queryArguments: QueryArguments) = SimpleMongoAction { database =>
    //    val queryArgFilter = queryArguments match {
//      case Some(arg) => arg.filter
//      case None      => None
//    }
//
//    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)
//
//    val cursorCondition = CursorConditionBuilder.buildCursorCondition(queryArguments)
//    We could try passing the other args into countoptions, but not sure about order
//    val baseQuery2                               = collection.countDocuments(Filters.and(buildConditionForFilter(queryArgFilter), cursorCondition)).toFuture()
//
//    val baseQuery: FindObservable[Document]      = collection(Filters.and(buildConditionForFilter(queryArgFilter), cursorCondition))
//    val queryWithOrder: FindObservable[Document] = OrderByClauseBuilder.queryWithOrder(baseQuery, queryArguments)
//    val queryWithSkip: FindObservable[Document]  = queryWithOrder.skip(skipAndLimit.skip)
//
//    val queryWithLimit = skipAndLimit.limit match {
//      case Some(limit) => queryWithSkip.limit(limit)
//      case None        => queryWithSkip
//    }
//
//    queryWithLimit.collect().toFuture

    database.getCollection(model.dbName).countDocuments(buildConditionForFilter(queryArguments.filter)).toFuture.map(_.toInt)
  }

  def countFromTable(table: String, filter: Option[Filter]) = SimpleMongoAction { database =>
    database.getCollection(table).countDocuments(buildConditionForFilter(filter)).toFuture.map(_.toInt)
  }

}
