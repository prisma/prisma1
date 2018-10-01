package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{CursorConditionBuilder, FilterConditionBuilder, OrderByClauseBuilder}
import com.prisma.api.connector.mongo.extensions.DocumentToRoot
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.whereToBson
import com.prisma.api.helpers.LimitClauseHelper
import com.prisma.gc_values._
import com.prisma.shared.models._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Document, FindObservable, MongoClient, MongoCollection}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

case class MongoDataResolver(project: Project, client: MongoClient)(implicit ec: ExecutionContext) extends DataResolver with FilterConditionBuilder {
  val database = client.getDatabase(project.id)

  override def getModelForGlobalId(globalId: CuidGCValue): Future[Option[Model]] = {
    val outer = project.models.map { model =>
      val collection: MongoCollection[Document] = database.getCollection(model.name)
      collection.find(Filters.eq("_id", globalId.value)).collect().toFuture.map { results: Seq[Document] =>
        if (results.nonEmpty) Vector(model) else Vector.empty
      }
    }

    val sequence: Future[List[Vector[Model]]] = Future.sequence(outer)

    sequence.map(_.flatten.headOption)
  }

  //Fixme this does not use selected fields
  override def getNodeByWhere(where: NodeSelector, selectedFields: SelectedFields): Future[Option[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(where.model.dbName)
    collection.find(where).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(where.model, result)
        PrismaNode(root.idField, root, Some(where.model.name))
      }
    }
  }

  override def countByTable(table: String, whereFilter: Option[Filter]): Future[Int] = {
    database.getCollection(table).countDocuments(buildConditionForFilter(whereFilter)).toFuture.map(_.toInt)
  }

  // Fixme this does not use selected fields
  override def getNodes(model: Model, queryArguments: Option[QueryArguments], selectedFields: SelectedFields): Future[ResolverResult[PrismaNode]] = {
    val collection: MongoCollection[Document] = database.getCollection(model.dbName)
    val filter = queryArguments match {
      case Some(arg) => arg.filter
      case None      => None
    }

    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)

    val mongoFilter = buildConditionForFilter(filter)

    val cursorCondition = CursorConditionBuilder.buildCursorCondition(queryArguments)

    val baseQuery: FindObservable[Document]      = collection.find(Filters.and(mongoFilter, cursorCondition))
    val queryWithOrder: FindObservable[Document] = OrderByClauseBuilder.queryWithOrder(baseQuery, queryArguments)
    val queryWithSkip: FindObservable[Document]  = queryWithOrder.skip(skipAndLimit.skip)

    val queryWithLimit = skipAndLimit.limit match {
      case Some(limit) => queryWithSkip.limit(limit)
      case None        => queryWithSkip
    }

    val nodes: Future[Seq[PrismaNode]] = queryWithLimit.collect().toFuture.map { results: Seq[Document] =>
      results.map { result =>
        val root = DocumentToRoot(model, result)
        PrismaNode(root.idField, root, Some(model.name))
      }
    }

    nodes.map(n => ResolverResult[PrismaNode](queryArguments, n.toVector))
  }

  //these are only used for relations between non-embedded types
  override def getRelatedNodes(fromField: RelationField,
                               fromNodeIds: Vector[IdGCValue],
                               queryArguments: Option[QueryArguments],
                               selectedFields: SelectedFields): Future[Vector[ResolverResult[PrismaNodeWithParent]]] = {

    val model                                 = fromField.relatedModel_!
    val manifestation                         = fromField.relation.inlineManifestation.get
    val collection: MongoCollection[Document] = database.getCollection(model.dbName)
    val inFilter                              = ScalarFilter(model.getScalarFieldByName_!("id").copy(manifestation.referencingColumn), Equals(fromNodeIds.head))
    val queryArgFilter = queryArguments match {
      case Some(arg) => arg.filter
      case None      => None
    }

    val skipAndLimit = LimitClauseHelper.skipAndLimitValues(queryArguments)

    val mongoFilter = buildConditionForFilter(Some(AndFilter(Vector(inFilter) ++ queryArgFilter)))

    val cursorCondition = CursorConditionBuilder.buildCursorCondition(queryArguments)

    val baseQuery: FindObservable[Document]      = collection.find(Filters.and(mongoFilter, cursorCondition))
    val queryWithOrder: FindObservable[Document] = OrderByClauseBuilder.queryWithOrder(baseQuery, queryArguments)
    val queryWithSkip: FindObservable[Document]  = queryWithOrder.skip(skipAndLimit.skip)

    val queryWithLimit = skipAndLimit.limit match {
      case Some(limit) => queryWithSkip.limit(limit)
      case None        => queryWithSkip
    }

    val queryResult = queryWithLimit.collect().toFuture

    queryResult.map { results: Seq[Document] =>
      val groups = fromField.relatedField.isList match {
        case true =>
          val tuples = for {
            result <- results
            id     <- result(manifestation.referencingColumn).asArray().getValues.asScala.map(_.asString()).map(x => CuidGCValue(x.getValue))
          } yield (id, result)
          tuples.groupBy(_._1).mapValues(_.map(_._2))

        case false => results.groupBy(x => CuidGCValue(x(manifestation.referencingColumn).asString().getValue))
      }

      val res: Vector[ResolverResult[PrismaNodeWithParent]] = groups.map { group =>
        val parentId                                  = fromNodeIds.find(_ == group._1).get
        val roots                                     = group._2.map(DocumentToRoot(model, _))
        val prismaNodes: Vector[PrismaNodeWithParent] = roots.map(r => PrismaNodeWithParent(parentId, PrismaNode(r.idField, r, Some(model.name)))).toVector
        ResolverResult(prismaNodes, hasPreviousPage = false, hasNextPage = false, parentModelId = Some(parentId))
      }.toVector

      val ids = res.flatMap(_.parentModelId)

      val idsNotCovered = fromNodeIds.filter(x => !ids.contains(x))
      val emptyResultsForNotCoveredIds =
        idsNotCovered.map(id => ResolverResult(Vector.empty[PrismaNodeWithParent], hasPreviousPage = false, hasNextPage = false, parentModelId = Some(id)))

      res ++ emptyResultsForNotCoveredIds
    }
  }

  override def getRelationNodes(relationTableName: String, queryArguments: Option[QueryArguments]): Future[ResolverResult[RelationNode]] = ???

  override def countByModel(model: Model, queryArguments: Option[QueryArguments]): Future[Int] = {
    countByTable(model.dbName, queryArguments.flatMap(_.filter))
  }

  // these should never be used and are only in here due to the interface
  override def getScalarListValues(model: Model, listField: ScalarField, queryArguments: Option[QueryArguments]): Future[ResolverResult[ScalarListValues]] = ???
  override def getScalarListValuesByNodeIds(model: Model, listField: ScalarField, nodeIds: Vector[IdGCValue]): Future[Vector[ScalarListValues]]            = ???
}
