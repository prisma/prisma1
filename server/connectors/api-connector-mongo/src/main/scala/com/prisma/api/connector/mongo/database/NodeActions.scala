package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.api.connector.mongo.extensions.Path
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{FieldCannotBeNull, NodesNotConnectedError}
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, RelationField}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonValue}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates._

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait NodeActions extends NodeSingleQueries {

  //region Top Level

  def createNode(mutaction: CreateNode, inlineRelations: List[(String, GCValue)])(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val (docWithId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(inlineRelations, mutaction)

      database.getCollection(mutaction.model.dbName).insertOne(docWithId).toFuture().map(_ => MutactionResults(childResults))
    }

  def deleteNodeById(model: Model, id: IdGCValue) = deleteNodes(model, Vector(id))

  def deleteNodes(model: Model, ids: Seq[IdGCValue]) = SimpleMongoAction { database =>
    database.getCollection(model.dbName).deleteMany(in("_id", ids.map(_.value): _*)).toFuture()
  }

  def updateNode(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext): MongoAction[MutactionResults] = {
    updateNodeByWhere(mutaction, mutaction.where)
  }

  def updateNodeByWhere(mutaction: UpdateNode, where: NodeSelector)(implicit ec: ExecutionContext) = {
    for {
      previousValues <- getNodeByWhere(where, SelectedFields.all(mutaction.model))
      results        <- updateHelper(mutaction, where, previousValues)
    } yield results
  }

  def updateHelper(mutaction: UpdateNode, where: NodeSelector, previousValues: Option[PrismaNode])(implicit ec: ExecutionContext) = SimpleMongoAction {
    database =>
      previousValues match {
        case None =>
          throw APIErrors.NodeNotFoundForWhereError(where)

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
            val combinedUpdates = CustomUpdateCombiner.customCombine(allUpdates)

            val updateOptions = UpdateOptions().arrayFilters((arrayFilters ++ arrayFilters2).toList.asJava)
            database
              .getCollection(mutaction.model.dbName)
              .updateOne(where, combinedUpdates, updateOptions)
              .toFuture()
              .map(_ => MutactionResults(results))
          }
      }
  }

  def updateNodes(mutaction: UpdateNodes, ids: Seq[IdGCValue]) = SimpleMongoAction { database =>
    val scalarUpdates   = scalarUpdateValues(mutaction)
    val combinedUpdates = CustomUpdateCombiner.customCombine(scalarUpdates)

    database.getCollection(mutaction.model.dbName).updateMany(in("_id", ids.map(_.value): _*), combinedUpdates).toFuture()
  }

  //endregion

  private def createToDoc(inlineRelations: List[(String, GCValue)],
                          mutaction: CreateNode,
                          results: Vector[DatabaseMutactionResult] = Vector.empty): (Document, Vector[DatabaseMutactionResult]) = {

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

    val nonListValues = mutaction.nonListArgs.raw.asRoot.map.map { case (f, v) => set(path.stringForField(f), GCToBson(v)) }.toVector
    val listValues    = mutaction.listArgs.map { case (f, v) => set(path.stringForField(f), GCToBson(v)) }

    nonListValues ++ listValues
  }

  private def embeddedNestedUpdateDocsAndResults(node: PrismaNode,
                                                 mutactions: Vector[UpdateNode],
                                                 path: Path = Path.empty): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {

    val actionsArrayFiltersAndResults = mutactions.collect {
      case toOneUpdate @ NestedUpdateNode(_, rf, None, _, _, _, _, _, _, _, _) if rf.relatedModel_!.isEmbedded =>
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

      case toManyUpdate @ NestedUpdateNode(_, rf, Some(where), _, _, _, _, _, _, _, _) if rf.relatedModel_!.isEmbedded =>
        val updatedPath = path.append(rf, where)
        val subNode = node.getToManyChild(rf, where) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, None, rf.relatedModel_!, Some(where))
          case Some(prismaNode) => prismaNode
        }

        val scalars                                      = scalarUpdateValues(toManyUpdate, updatedPath)
        val (creates, createResults)                     = embeddedNestedCreateActionsAndResults(toManyUpdate, updatedPath)
        val (updates, nestedArrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(subNode, toManyUpdate.nestedUpdates, updatedPath)
        val (deletes, deleteResults)                     = embeddedNestedDeleteActionsAndResults(subNode, toManyUpdate, updatedPath)
        val thisResult                                   = UpdateNodeResult(subNode.id, subNode, toManyUpdate)

        (scalars ++ creates ++ deletes ++ updates, updatedPath.arrayFilter ++ nestedArrayFilters, createResults ++ deleteResults ++ updateResults :+ thisResult)
    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  private def embeddedNestedUpsertDocsAndResults(node: PrismaNode,
                                                 mutaction: UpdateNode,
                                                 path: Path = Path.empty): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {
    val actionsArrayFiltersAndResults = mutaction.nestedUpserts.collect {
      case toOneUpsert @ NestedUpsertNode(_, rf, None, create, update) if rf.relatedModel_!.isEmbedded =>
        node.getToOneChild(rf) match {
          case None =>
            val (createDoc, createResults) = createToDoc(List.empty, create)
            (Vector(push(rf.name, createDoc)), Vector.empty, createResults :+ UpsertNodeResult(toOneUpsert, toOneUpsert))

          case Some(_) =>
            val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(node, Vector(update), path)
            (updates, arrayFilters, updateResults :+ UpsertNodeResult(toOneUpsert, toOneUpsert))
        }

      case toManyUpsert @ NestedUpsertNode(_, rf, Some(where), create, update) if rf.relatedModel_!.isEmbedded =>
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

  private def embeddedNestedCreateActionsAndResults(mutaction: FurtherNestedMutaction,
                                                    path: Path = Path.empty): (Vector[Bson], Vector[DatabaseMutactionResult]) = {
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
}
