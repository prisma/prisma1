package com.prisma.api.connector.mongo.database

import com.mongodb.MongoClientSettings
import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.api.connector.mongo.extensions.Path
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{FieldCannotBeNull, NodesNotConnectedError}
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, RelationField}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonValue, conversions}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.{Document, MongoCollection}

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait NodeActions extends NodeSingleQueries {

  //region Top Level

  def createNode(mutaction: CreateNode, inlineRelations: List[(String, GCValue)])(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection: MongoCollection[Document]                                = database.getCollection(mutaction.model.dbName)
      val (docWithId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(inlineRelations, mutaction)

      collection.insertOne(docWithId).toFuture().map(_ => MutactionResults(childResults))
    }

  def deleteNode(mutaction: TopLevelDeleteNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document]      = database.getCollection(mutaction.model.dbName)
    val previousValues: Future[Option[PrismaNode]] = getNodeByWhere(mutaction.where, SelectedFields.all(mutaction.model), database)

    previousValues.flatMap {
      case Some(node) => collection.deleteOne(mutaction.where).toFuture().map(_ => MutactionResults(Vector(DeleteNodeResult(node.id, node, mutaction))))
      case None       => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
    }
  }

  def deleteNodes(mutaction: DeleteNodes)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection                        = database.getCollection(mutaction.model.dbName)
      val futureIds: Future[Seq[IdGCValue]] = getNodeIdsByFilter(mutaction.model, mutaction.whereFilter, database)

      futureIds.flatMap { ids =>
        val results = ManyNodesResult(mutaction, ids.size)
        collection.deleteMany(in("_id", ids.map(_.value): _*)).toFuture().map(_ => MutactionResults(Vector(results)))
      }
    }

  def updateNode(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document]      = database.getCollection(mutaction.model.dbName)
    val previousValues: Future[Option[PrismaNode]] = getNodeByWhere(mutaction.where, database)

    previousValues.flatMap {
      case None =>
        throw APIErrors.NodeNotFoundForWhereError(mutaction.where)

      case Some(node) =>
        val scalarUpdates                           = scalarUpdateValues(mutaction)
        val (creates, createResults)                = embeddedNestedCreateActionsAndResults(mutaction)
        val (deletes, deleteResults)                = embeddedNestedDeleteActionsAndResults(node, mutaction)
        val (updates, arrayFilters, updateResults)  = embeddedNestedUpdateDocsAndResults(node, mutaction.nestedUpdates)
        val (upserts, arrayFilters2, upsertResults) = embeddedNestedUpsertDocsAndResults(node, mutaction)

        val allUpdates = scalarUpdates ++ creates ++ deletes ++ updates ++ upserts

        val results = createResults ++ deleteResults ++ updateResults ++ upsertResults :+ UpdateNodeResult(node.id, node, mutaction)
        if (allUpdates.isEmpty) {
          Future.successful(MutactionResults(results))
        } else {
          val combinedUpdates = customCombine(allUpdates)

          val updateOptions = UpdateOptions().arrayFilters((arrayFilters ++ arrayFilters2).toList.asJava)
          collection
            .updateOne(mutaction.where, combinedUpdates, updateOptions)
            .toFuture()
            .map(_ => MutactionResults(results))
        }
    }
  }

  def updateNodes(mutaction: UpdateNodes)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection                        = database.getCollection(mutaction.model.dbName)
      val futureIds: Future[Seq[IdGCValue]] = getNodeIdsByFilter(mutaction.model, mutaction.whereFilter, database)
      val scalarUpdates                     = scalarUpdateValues(mutaction)
      val combinedUpdates                   = customCombine(scalarUpdates)

      futureIds.flatMap { ids =>
        val results = ManyNodesResult(mutaction, ids.size)
        collection.updateMany(in("_id", ids.map(_.value): _*), combinedUpdates).toFuture().map(_ => MutactionResults(Vector(results)))
      }
    }

  def upsertNode(mutaction: TopLevelUpsertNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document]      = database.getCollection(mutaction.where.model.dbName)
    val previousValues: Future[Option[PrismaNode]] = getNodeByWhere(mutaction.where, database)

    previousValues.flatMap {
      case None =>
        val (docWithId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(List.empty, mutaction.create)

        collection.insertOne(docWithId).toFuture().map(_ => MutactionResults(Vector(UpsertNodeResult(mutaction.create, mutaction)) ++ childResults))

      //how does returning the created id work?

      case Some(node) =>
        topLevelUpdateHelper(mutaction.update, collection, node)
    }
  }

  //endregion

  //region    Nested for nonEmbedded relations, not implemented yet

  private def topLevelUpdateHelper(mutaction: TopLevelUpdateNode, collection: MongoCollection[Document], node: PrismaNode)(implicit ec: ExecutionContext) = {
    val scalarUpdates                          = scalarUpdateValues(mutaction)
    val (creates, createResults)               = embeddedNestedCreateActionsAndResults(mutaction)
    val (deletes, deleteResults)               = embeddedNestedDeleteActionsAndResults(node, mutaction)
    val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(node, mutaction.nestedUpdates)

    val combinedUpdates = customCombine(scalarUpdates ++ creates ++ deletes ++ updates)

    val updateOptions = UpdateOptions().arrayFilters(arrayFilters.toList.asJava)

    val results = createResults ++ deleteResults ++ updateResults :+ UpdateNodeResult(node.id, node, mutaction)

    collection
      .updateOne(mutaction.where, combinedUpdates, updateOptions)
      .toFuture()
      .map(_ => MutactionResults(results))
  }

  def nestedCreateNode(mutaction: NestedCreateNode, parentId: IdGCValue)(implicit ec: ExecutionContext): MongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      ???
    }

  def nestedDeleteNode(mutaction: NestedDeleteNode, parentId: IdGCValue)(implicit ec: ExecutionContext): MongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val childModel      = mutaction.model
      val parentModel     = mutaction.relationField.model
      val childCollection = database.getCollection(childModel.dbName)

      mutaction.relationField.relation.inlineManifestation match {
        case Some(m) if m.inTableOfModelId == childModel.name =>
          val filter = ScalarFilter(childModel.idField_!.copy(name = m.referencingColumn), Equals(parentId))

          val mongoFilter = mutaction.where match {
            case Some(where) => buildConditionForFilter(Some(AndFilter(Vector(filter, ScalarFilter(where.field, Equals(where.fieldGCValue))))))
            case None        => buildConditionForFilter(Some(filter))
          }

          val previousValues = getNodeByFilter(childModel, mongoFilter, database)

          previousValues.flatMap {
            case Some(node) =>
              childCollection.deleteOne(mongoFilter).toFuture().map(_ => MutactionResults(Vector(DeleteNodeResult(node.id, node, mutaction))))

            case None => throw NodesNotConnectedError(mutaction.relationField.relation, parentModel, None, childModel, None)
          }
        case Some(m) if m.inTableOfModelId == parentModel.name =>
          mutaction.where match {
            case Some(where) => ??? // get the child ids from the parent and then delete the child that fits id + where
            case None        => ??? // get the child Id from the parent and then delete the child by id
          }

        case _ => ???
      }
    }

  def nestedUpdateNode(mutaction: NestedUpdateNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      ???
    }

  def nestedUpsertNode(mutaction: NestedUpsertNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      ???
    }

  //endregion

  private def createToDoc(inlineRelations: List[(String, GCValue)],
                          mutaction: CreateNode,
                          results: Vector[DatabaseMutactionResult] = Vector.empty): (Document, Vector[DatabaseMutactionResult]) = {

    //Fixme only do embedded stuff for embedded models

    val nonListValues: List[(String, GCValue)] =
      mutaction.model.scalarNonListFields
        .filter(field => mutaction.nonListArgs.hasArgFor(field) && mutaction.nonListArgs.getFieldValue(field.name).get != NullGCValue)
        .map(field => field.name -> mutaction.nonListArgs.getFieldValue(field).get)

    val id                                 = CuidGCValue.random
    val nonListArgsWithId                  = nonListValues :+ ("_id", id)
    val (nestedCreateFields, childResults) = embeddedNestedCreateDocsAndResults(mutaction)
    val thisResult                         = CreateNodeResult(id, mutaction)
    val doc                                = Document(nonListArgsWithId ++ mutaction.listArgs ++ inlineRelations) ++ nestedCreateFields

    (doc, childResults :+ thisResult)
  }

  private def embeddedNestedCreateDocsAndResults(mutaction: FurtherNestedMutaction): (Map[String, BsonValue], Vector[DatabaseMutactionResult]) = {
    val (childResults: Vector[DatabaseMutactionResult], grouped: Map[RelationField, immutable.Seq[Document]]) = nestedCreateDocAndResultHelper(mutaction)
    val nestedCreateFields = grouped.foldLeft(Map.empty[String, BsonValue]) { (map, t) =>
      val rf: RelationField = t._1
      val documents         = t._2.map(_.toBsonDocument)

      if (rf.isList) map + (rf.name -> BsonArray(documents)) else map + (rf.name -> documents.head)
    }

    (nestedCreateFields, childResults)
  }

  private def embeddedNestedCreateDocsAndResultsThatCanBeWithinUpdate(
      mutaction: FurtherNestedMutaction): (Map[RelationField, Vector[BsonDocument]], Vector[DatabaseMutactionResult]) = {
    val (childResults, grouped) = nestedCreateDocAndResultHelper(mutaction)
    val nestedCreateFields      = grouped.map { case (f, v) => (f, v.map(_.toBsonDocument).toVector) }

    (nestedCreateFields, childResults)
  }

  private def nestedCreateDocAndResultHelper(mutaction: FurtherNestedMutaction) = {
    val nestedCreates                                        = mutaction.nestedCreates.collect { case m if m.relationField.relatedModel_!.isEmbedded => m.relationField -> createToDoc(List.empty, m) }
    val childResults: Vector[DatabaseMutactionResult]        = nestedCreates.flatMap(x => x._2._2)
    val grouped: Map[RelationField, immutable.Seq[Document]] = nestedCreates.groupBy(_._1).mapValues(_.map(_._2._1))
    (childResults, grouped)
  }

  private def embeddedNestedDeleteActionsAndResults(node: PrismaNode,
                                                    mutaction: UpdateNode,
                                                    path: Path = Path.empty): (Vector[Bson], Vector[DeleteNodeResult]) = {
    val parentWhere = mutaction match {
      case top: TopLevelUpdateNode  => Some(top.where)
      case nested: NestedUpdateNode => nested.where
    }

    val actionsAndResults = mutaction.nestedDeletes.collect {
      case toOneDelete @ NestedDeleteNode(_, rf, None) if rf.relatedModel_!.isEmbedded =>
        node.getToOneChild(rf) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, parentWhere, toOneDelete.model, None)
          case Some(nestedNode) => (unset(path.stringForField(rf.name)), DeleteNodeResult(CuidGCValue.dummy, nestedNode, toOneDelete))
        }

      case toManyDelete @ NestedDeleteNode(_, rf, Some(where)) if rf.relatedModel_!.isEmbedded =>
        node.getToManyChild(rf, where) match {
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
                                                 mutactions: Vector[UpdateNode],
                                                 path: Path = Path.empty): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {

    val actionsArrayFiltersAndResults = mutactions.collect {
      case toOneUpdate @ NestedUpdateNode(_, rf, None, _, _, _, _, _, _, _, _) =>
        val updatedPath = path.append(rf)
        val subNode = node.getToOneChild(rf) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, None, rf.relatedModel_!, None)
          case Some(prismaNode) => prismaNode
        }

        val scalars                                = scalarUpdateValues(toOneUpdate, updatedPath)
        val (creates, createResults)               = embeddedNestedCreateActionsAndResults(toOneUpdate, updatedPath)
        val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(subNode, toOneUpdate.nestedUpdates, updatedPath)
        val (deletes, deleteResults)               = embeddedNestedDeleteActionsAndResults(subNode, toOneUpdate, updatedPath)
        val thisResult                             = UpdateNodeResult(subNode.id, subNode, toOneUpdate)

        (scalars ++ creates ++ deletes ++ updates, arrayFilters, createResults ++ deleteResults ++ updateResults :+ thisResult)

      case toManyUpdate @ NestedUpdateNode(_, rf, Some(where), _, _, _, _, _, _, _, _) =>
        val updatedPath = path.append(rf, where)
        val subNode = node.getToManyChild(rf, where) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, None, rf.relatedModel_!, Some(where))
          case Some(prismaNode) => prismaNode
        }

        val arrayFilters = updatedPath.arrayFilter //Fixme this is not always needed i think, check if this errors if it is unused

        val scalars                                      = scalarUpdateValues(toManyUpdate, updatedPath)
        val (creates, createResults)                     = embeddedNestedCreateActionsAndResults(toManyUpdate, updatedPath)
        val (updates, nestedArrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(subNode, toManyUpdate.nestedUpdates, updatedPath)
        val (deletes, deleteResults)                     = embeddedNestedDeleteActionsAndResults(subNode, toManyUpdate, updatedPath)
        val thisResult                                   = UpdateNodeResult(subNode.id, subNode, toManyUpdate)

        (scalars ++ creates ++ deletes ++ updates, arrayFilters ++ nestedArrayFilters, createResults ++ deleteResults ++ updateResults :+ thisResult)
    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  private def embeddedNestedUpsertDocsAndResults(node: PrismaNode,
                                                 mutaction: UpdateNode,
                                                 path: Path = Path.empty): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {
    val actionsArrayFiltersAndResults = mutaction.nestedUpserts.collect {
      case toOneUpsert @ NestedUpsertNode(_, rf, None, create, update) =>
        node.getToOneChild(rf) match {
          case None =>
            val (createDoc, createResults) = createToDoc(List.empty, create)
            (Vector(push(rf.name, createDoc)), Vector.empty, createResults :+ UpsertNodeResult(toOneUpsert, toOneUpsert))

          case Some(_) =>
            val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(node, Vector(update), path)
            (updates, arrayFilters, updateResults :+ UpsertNodeResult(toOneUpsert, toOneUpsert))
        }

      case toManyUpsert @ NestedUpsertNode(_, rf, Some(where), create, update) =>
        node.getToManyChild(rf, where) match {
          case None =>
            val (createDoc, createResults) = createToDoc(List.empty, create)
            (Vector(push(rf.name, createDoc)), Vector.empty, createResults :+ UpsertNodeResult(toManyUpsert, toManyUpsert))

          case Some(_) =>
            val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(node, Vector(update), path)
            (updates, arrayFilters, updateResults :+ UpsertNodeResult(toManyUpsert, toManyUpsert))

        }

    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  def embeddedNestedCreateActionsAndResults(mutaction: FurtherNestedMutaction, path: Path = Path.empty): (Vector[Bson], Vector[DatabaseMutactionResult]) = {
    mutaction match {
      case x: CreateNode =>
        val (nestedCreateFields, nestedCreateResults) = embeddedNestedCreateDocsAndResults(mutaction)
        val nestedCreates                             = nestedCreateFields.map { case (f, v) => set(path.stringForField(f), v) }.toVector
        (nestedCreates, nestedCreateResults)

      case x: UpdateNode =>
        val (nestedCreateFields: Map[RelationField, Vector[BsonDocument]], nestedCreateResults) = embeddedNestedCreateDocsAndResultsThatCanBeWithinUpdate(
          mutaction)
        val nestedCreates = nestedCreateFields.collect {
          case (f, v) if !f.isList => set(path.stringForField(f.name), v.head)
          case (f, v) if f.isList  => pushEach(path.stringForField(f.name), v: _*)
        }.toVector
        (nestedCreates, nestedCreateResults)
    }
  }

  def deleteNodeById(model: Model, id: IdGCValue) = deleteNodesByIds(model, Vector(id))

  def deleteNodesByIds(model: Model, ids: Vector[IdGCValue]) = SimpleMongoAction { database =>
    val mongoFilter = buildConditionForFilter(Some(ScalarFilter(model.idField_!, In(ids))))
    database.getCollection(model.dbName).deleteMany(mongoFilter).collect().toFuture()
  }

  // helpers

  private def customCombine(updates: Vector[conversions.Bson]): conversions.Bson = {
    val rawUpdates = updates.map(update => (update.toBsonDocument(classOf[Document], MongoClientSettings.getDefaultCodecRegistry), update))

    val pulls  = rawUpdates.filter(_._1.getFirstKey == "$pull")
    val others = rawUpdates.filter(_._1.getFirstKey != "$pull")

    val convertedPulls                                                    = pulls.map(x => documentToCombinedPullDefinition(x._1))
    val groupedPulls: Map[Vector[String], Vector[CombinedPullDefinition]] = convertedPulls.groupBy(_.keys)

    val changedPulls = groupedPulls.map { group =>
      val array = BsonArray(group._2.map(_.value))

      bsonDocumentFilter(group._1.toList, array)
    }

    combine(others.map(_._2) ++ changedPulls: _*)
  }

  def bsonDocumentFilter(keys: List[String], array: BsonArray): Document = keys match {
    case Nil          => sys.error("should not happen")
    case head :: Nil  => BsonDocument(head -> BsonDocument("$in" -> array))
    case head :: tail => BsonDocument(head -> bsonDocumentFilter(tail, array))
  }

  def documentToCombinedPullDefinition(doc: Document, keys: Vector[String] = Vector.empty): CombinedPullDefinition = {
    val key     = doc.keys.head
    val value   = doc.get(key).get
    val newKeys = keys :+ key
    if (value.isDocument) {
      documentToCombinedPullDefinition(value.asDocument(), newKeys)
    } else {
      CombinedPullDefinition(newKeys, value)
    }
  }

  case class CombinedPullDefinition(keys: Vector[String], value: BsonValue)
}
