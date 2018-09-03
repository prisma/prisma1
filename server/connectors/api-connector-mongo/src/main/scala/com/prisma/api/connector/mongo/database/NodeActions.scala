package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.api.connector.mongo.extensions.Path
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{FieldCannotBeNull, NodesNotConnectedError}
import com.prisma.gc_values._
import com.prisma.shared.models.RelationField
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonValue, conversions}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.{Document, MongoCollection}
import collection.JavaConverters._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait NodeActions extends NodeSingleQueries {

  //region Top Level

  def createNode(mutaction: CreateNode, includeRelayRow: Boolean)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection: MongoCollection[Document]                                               = database.getCollection(mutaction.model.dbName)
      val (id: IdGCValue, docWithId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(mutaction)

      collection.insertOne(docWithId).toFuture().map(_ => MutactionResults(Vector(CreateNodeResult(id, mutaction)) ++ childResults))
    }

  def deleteNode(mutaction: TopLevelDeleteNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document]      = database.getCollection(mutaction.model.name)
    val previousValues: Future[Option[PrismaNode]] = getNodeByWhere(mutaction.where, SelectedFields.all(mutaction.model), database)

    previousValues.flatMap {
      case Some(node) => collection.deleteOne(mutaction.where).toFuture().map(_ => MutactionResults(Vector(DeleteNodeResult(node.id, node, mutaction))))
      case None       => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
    }
  }

  def deleteNodes(mutaction: DeleteNodes, shouldDeleteRelayIds: Boolean)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection                        = database.getCollection(mutaction.model.name)
      val futureIds: Future[Seq[IdGCValue]] = getNodeIdsByFilter(mutaction.model, mutaction.whereFilter, database)

      futureIds.flatMap(ids => collection.deleteMany(in("_id", ids.map(_.value): _*)).toFuture().map(_ => MutactionResults(Vector.empty)))
    }

  def updateNode(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document]      = database.getCollection(mutaction.model.name)
    val previousValues: Future[Option[PrismaNode]] = getNodeByWhere(mutaction.where, database)

    previousValues.flatMap {
      case None =>
        throw APIErrors.NodeNotFoundForWhereError(mutaction.where)

      case Some(node) =>
        val scalarUpdates                          = scalarUpdateValues(mutaction)
        val (creates, createResults)               = embeddedNestedCreateActionsAndResults(mutaction)
        val (deletes, deleteResults)               = embeddedNestedDeleteActionsAndResults(node, mutaction)
        val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(node, mutaction)

        val combinedUpdates = combine(customCombine(scalarUpdates ++ creates ++ deletes ++ updates): _*)
        val updateOptions   = UpdateOptions().arrayFilters(arrayFilters.toList.asJava)
        val results         = createResults ++ deleteResults ++ updateResults :+ UpdateNodeResult(node.id, node, mutaction)

        collection
          .updateOne(mutaction.where, combinedUpdates, updateOptions)
          .toFuture()
          .map(_ => MutactionResults(results))
    }
  }

  def updateNodes(mutaction: UpdateNodes)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection                        = database.getCollection(mutaction.model.name)
      val futureIds: Future[Seq[IdGCValue]] = getNodeIdsByFilter(mutaction.model, mutaction.whereFilter, database)

      val scalarUpdates = scalarUpdateValues(mutaction)

      val combinedUpdates = combine(customCombine(scalarUpdates): _*)
      val results         = ManyNodesResult(mutaction)

      futureIds.flatMap(ids => collection.updateMany(in("_id", ids.map(_.value): _*), combinedUpdates).toFuture().map(_ => MutactionResults(Vector(results))))
    }

  //endregion

  //region    Nested for nonEmbedded relations, not implemented yet

  def nestedCreateNode(mutaction: NestedCreateNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      ???
    }

  def nestedDeleteNode(mutaction: NestedDeleteNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
//      mutaction.where match {
//        case None => sys.error("Only toMany deletes should arrive here.")
//        case Some(nodeSelector) =>
//          val parentModel                           = mutaction.relationField.model
//          val collection: MongoCollection[Document] = database.getCollection(parentModel.name)
//
//          collection
//            .updateOne(equal("_id", parentId.value), pull(mutaction.relationField.name, nodeSelector))
//            .toFuture()
//            .map(_ => MutactionResults(Vector.empty))
//
//      }
      ???
    }

  def nestedUpdateNode(mutaction: NestedUpdateNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      ???
    }

  //endregion

  private def createToDoc(mutaction: CreateNode,
                          results: Vector[DatabaseMutactionResult] = Vector.empty): (IdGCValue, Document, Vector[DatabaseMutactionResult]) = {

    val nonListValues: List[(String, GCValue)] =
      mutaction.model.scalarNonListFields
        .filter(field => mutaction.nonListArgs.hasArgFor(field) && mutaction.nonListArgs.getFieldValue(field.name).get != NullGCValue)
        .map(field => field.name -> mutaction.nonListArgs.getFieldValue(field).get)

    val id                                 = CuidGCValue.random
    val nonListArgsWithId                  = nonListValues :+ ("_id", id)
    val (nestedCreateFields, childResults) = embeddedNestedCreateDocsAndResults(mutaction)
    val thisResult                         = CreateNodeResult(id, mutaction)
    val doc                                = Document(nonListArgsWithId ++ mutaction.listArgs) ++ nestedCreateFields

    (id, doc, childResults :+ thisResult)
  }

  private def embeddedNestedCreateDocsAndResults(mutaction: FurtherNestedMutaction): (Map[String, BsonValue], Vector[DatabaseMutactionResult]) = {
    val nestedCreates                                        = mutaction.nestedCreates.map(m => m.relationField -> createToDoc(m))
    val childResults: Vector[DatabaseMutactionResult]        = nestedCreates.flatMap(x => x._2._3)
    val grouped: Map[RelationField, immutable.Seq[Document]] = nestedCreates.groupBy(_._1).mapValues(_.map(_._2._2))

    val nestedCreateFields = grouped.foldLeft(Map.empty[String, BsonValue]) { (map, t) =>
      val rf: RelationField = t._1
      val documents         = t._2.map(_.toBsonDocument)

      if (rf.isList) map + (rf.name -> BsonArray(documents)) else map + (rf.name -> documents.head)
    }

    (nestedCreateFields, childResults)
  }

  private def embeddedNestedDeleteActionsAndResults(node: PrismaNode,
                                                    mutaction: UpdateNode,
                                                    path: Path = Path.empty): (Vector[Bson], Vector[DeleteNodeResult]) = {
    val parentWhere = mutaction match {
      case top: TopLevelUpdateNode  => Some(top.where)
      case nested: NestedUpdateNode => nested.where
    }

    val actionsAndResults = mutaction.nestedDeletes.map {
      case toOneDelete @ NestedDeleteNode(_, rf, None) =>
        node.toOneChild(rf) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, parentWhere, toOneDelete.model, None)
          case Some(nestedNode) => (unset(path.stringForField(rf.name)), DeleteNodeResult(CuidGCValue.dummy, nestedNode, toOneDelete))
        }

      case toManyDelete @ NestedDeleteNode(_, rf, Some(where)) =>
        node.toManyChild(rf, where) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, parentWhere, toManyDelete.model, Some(where))
          case Some(nestedNode) => (pull(path.stringForField(rf.name), whereToBson(where)), DeleteNodeResult(CuidGCValue.dummy, nestedNode, toManyDelete))
        }
    }

    (actionsAndResults.map(_._1), actionsAndResults.map(_._2))
  }

  private def scalarUpdateValues(mutaction: AllUpdateNodes, path: Path = Path.empty): Vector[Bson] = {
    val invalidUpdates = mutaction.nonListArgs.raw.asRoot.map.collect { case (k, v) if v == NullGCValue && mutaction.model.getFieldByName_!(k).isRequired => k }
    if (invalidUpdates.nonEmpty) throw FieldCannotBeNull(invalidUpdates.head)

    val nonListValues = mutaction.nonListArgs.raw.asRoot.map.map { case (f, v) => set(path.stringForField(f), GCValueBsonTransformer(v)) }.toVector
    val listValues    = mutaction.listArgs.map { case (f, v) => set(path.stringForField(f), GCValueBsonTransformer(v)) }

    nonListValues ++ listValues
  }

  private def embeddedNestedUpdateDocsAndResults(node: PrismaNode,
                                                 mutaction: UpdateNode,
                                                 path: Path = Path.empty): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {

    val actionsArrayFiltersAndResults = mutaction.nestedUpdates.collect {
      case toOneUpdate @ NestedUpdateNode(_, rf, None, _, _, _, _, _, _, _, _) =>
        val updatedPath = path.append(rf)
        val subNode = node.toOneChild(rf) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, None, rf.relatedModel_!, None)
          case Some(prismaNode) => prismaNode
        }

        val scalars                                = scalarUpdateValues(toOneUpdate, updatedPath)
        val (creates, createResults)               = embeddedNestedCreateActionsAndResults(toOneUpdate, updatedPath)
        val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(subNode, toOneUpdate, updatedPath)
        val (deletes, deleteResults)               = embeddedNestedDeleteActionsAndResults(subNode, toOneUpdate, updatedPath)
        val thisResult                             = UpdateNodeResult(subNode.id, subNode, toOneUpdate)

        (scalars ++ creates ++ deletes ++ updates, arrayFilters, createResults ++ deleteResults ++ updateResults :+ thisResult)

      case toManyUpdate @ NestedUpdateNode(_, rf, Some(where), _, _, _, _, _, _, _, _) =>
        val updatedPath = path.append(rf, where)
        val subNode = node.toManyChild(rf, where) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, None, rf.relatedModel_!, Some(where))
          case Some(prismaNode) => prismaNode
        }

        val arrayFilters = updatedPath.arrayFilter //Fixme this is not always needed i think, check if this errors if it is unused

        val scalars                                      = scalarUpdateValues(toManyUpdate, updatedPath)
        val (creates, createResults)                     = embeddedNestedCreateActionsAndResults(toManyUpdate, updatedPath)
        val (updates, nestedArrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(subNode, toManyUpdate, updatedPath)
        val (deletes, deleteResults)                     = embeddedNestedDeleteActionsAndResults(subNode, toManyUpdate, updatedPath)
        val thisResult                                   = UpdateNodeResult(subNode.id, subNode, toManyUpdate)

        (scalars ++ creates ++ deletes ++ updates, arrayFilters ++ nestedArrayFilters, createResults ++ deleteResults ++ updateResults :+ thisResult)
    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  //Fixme: this should probably be a push instead of a set
  def embeddedNestedCreateActionsAndResults(mutaction: FurtherNestedMutaction, path: Path = Path.empty): (Vector[Bson], Vector[DatabaseMutactionResult]) = {
    val (nestedCreateFields, nestedCreateResults) = embeddedNestedCreateDocsAndResults(mutaction)
    val nestedCreates                             = nestedCreateFields.map { case (f, v) => set(path.stringForField(f), v) }.toVector
    (nestedCreates, nestedCreateResults)
  }

  // helpers

  private def customCombine(updates: Vector[conversions.Bson]): Vector[conversions.Bson] = {
    //Fixme needs to combine updates that the Mongo DB combine would swallow since they reference the same field and value
    //{ "$pull" : { "comments" : { "alias" : "alias1" } } }
    //{ "$pull" : { "comments" : { "alias" : "alias2" } } }
    //{ "$pull" : { "comments" : { "alias" : {$in: ["alias1","alias2"]} } } }

    //in this case the second one would overwrite the first one -.-

//    updates.map { update =>
//      val res: Bson = update
//
//      val doc = update.toBsonDocument(classOf[Document], MongoClientSettings.getDefaultCodecRegistry)
//
//    }

    updates
  }
}
