package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.ArrayFilter
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCToBson
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer._
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{FieldCannotBeNull, NodesNotConnectedError}
import com.prisma.gc_values._
import com.prisma.shared.models.{Model, RelationField}
import org.bson.types.ObjectId
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
      val (docWithId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(inlineRelations, mutaction, None, None)

      database.getCollection(mutaction.model.dbName).insertOne(docWithId).toFuture().map(_ => MutactionResults(childResults))
    }

  def deleteNodeById(model: Model, id: IdGCValue) = deleteNodes(model, Vector(id))

  def deleteNodes(model: Model, ids: Seq[IdGCValue]) = SimpleMongoAction { database =>
    database.getCollection(model.dbName).deleteMany(in("_id", ids.map(x => GCToBson(x)): _*)).toFuture()
  }

  def updateNode(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext): MongoAction[MutactionResults] = {
    updateNodeByWhere(mutaction, mutaction.where)
  }

  def updateNodeByWhere(mutaction: UpdateNode, where: NodeSelector)(implicit ec: ExecutionContext) = {
    for {
      previousValues <- getNodeByWhereComplete(where)
      results        <- updateHelper(mutaction, where, previousValues)
    } yield results
  }

  def updateHelper(mutaction: UpdateNode, where: NodeSelector, previousValues: Option[PrismaNode])(implicit ec: ExecutionContext) = SimpleMongoAction {
    database =>
      previousValues match {
        case None =>
          throw APIErrors.NodeNotFoundForWhereError(where)

        case Some(node) =>
          val parent                                          = NodeAddress.forId(mutaction.model, node.id)
          val scalarUpdates                                   = scalarUpdateValues(mutaction, parent)
          val (creates, createResults)                        = embeddedNestedCreateActionsAndResults(mutaction, parent)
          val (deletes, deleteResults)                        = embeddedNestedDeleteActionsAndResults(node, mutaction, parent)
          val (updates, arrayFilters, updateResults)          = embeddedNestedUpdateDocsAndResults(node, mutaction.nestedUpdates, parent)
          val (updateManys, arrayFilters2, updateManyResults) = embeddedNestedUpdateManyDocsAndResults(node, mutaction.nestedUpdateManys, parent)
          val (deleteManys, arrayFilters3, deleteManyResults) = embeddedNestedDeleteManyDocsAndResults(node, mutaction.nestedDeleteManys, parent)
          val (upserts, arrayFilters4, upsertResults)         = embeddedNestedUpsertDocsAndResults(node, mutaction, parent)

          val allUpdates = scalarUpdates ++ creates ++ deletes ++ updates ++ upserts ++ updateManys ++ deleteManys

          val thisResult = UpdateNodeResult(node.id, node, mutaction)

          val results = createResults ++ deleteResults ++ updateResults ++ upsertResults ++ updateManyResults ++ deleteManyResults :+ thisResult
          if (allUpdates.isEmpty) {
            Future.successful(MutactionResults(results))
          } else {
            val combinedUpdates = CustomUpdateCombiner.customCombine(allUpdates)
            val allArrayFilters = arrayFilters ++ arrayFilters2 ++ arrayFilters3 ++ arrayFilters4
            val setArrayFilters = CustomUpdateCombiner.removeDuplicateArrayFilters(allArrayFilters)
            val updateOptions   = UpdateOptions().arrayFilters(setArrayFilters.asJava)

            database
              .getCollection(mutaction.model.dbName)
              .updateOne(where, combinedUpdates, updateOptions)
              .toFuture()
              .map(_ => MutactionResults(results))
          }
      }
  }

  def updateNodes(mutaction: AllUpdateNodes, ids: Seq[IdGCValue]) = SimpleMongoAction { database =>
    val nodeAddress = NodeAddress.forId(mutaction.model, StringIdGCValue.dummy)

    val scalarUpdates   = scalarUpdateValues(mutaction, nodeAddress)
    val combinedUpdates = CustomUpdateCombiner.customCombine(scalarUpdates)

    database.getCollection(mutaction.model.dbName).updateMany(in("_id", ids.map(x => GCToBson(x)): _*), combinedUpdates).toFuture()
  }

  //endregion

  private def createToDoc(inlineRelations: List[(String, GCValue)],
                          mutaction: CreateNode,
                          parent: Option[NodeAddress],
                          relationField: Option[RelationField],
                          results: Vector[DatabaseMutactionResult] = Vector.empty): (Document, Vector[DatabaseMutactionResult]) = {

    val nonListValues: List[(String, GCValue)] =
      mutaction.model.scalarNonListFields
        .filter(field => mutaction.nonListArgs.hasArgFor(field) && mutaction.nonListArgs.getFieldValue(field.name).get != NullGCValue)
        .map(field => field.dbName -> mutaction.nonListArgs.getFieldValue(field).get)

    val listValues: Vector[(String, GCValue)] = mutaction.listArgs.map { case (f, v) => (mutaction.model.getFieldByName_!(f).dbName, v) }

    val id = mutaction.nonListArgs.rootGCMap.get(mutaction.model.idField_!.name) match {
      case None     => StringIdGCValue(ObjectId.get().toString)
      case Some(id) => id
    }

    val currentParent: NodeAddress = (parent, relationField) match {
      case (Some(p), Some(rf)) if rf.isList  => p.appendPath(rf, NodeSelector.forId(mutaction.model, id.asInstanceOf[IdGCValue]))
      case (Some(p), Some(rf)) if !rf.isList => p.appendPath(rf)
      case (_, _)                            => NodeAddress.forId(mutaction.model, id.asInstanceOf[IdGCValue])
    }

    val nonListArgsWithId                  = nonListValues :+ ("_id", id)
    val (nestedCreateFields, childResults) = embeddedNestedCreateDocsAndResults(mutaction, currentParent)
    val doc                                = Document(nonListArgsWithId ++ listValues ++ inlineRelations) ++ nestedCreateFields

    (doc, childResults :+ CreateNodeResult(currentParent, mutaction))
  }

  private def embeddedNestedCreateDocsAndResults(mutaction: FurtherNestedMutaction,
                                                 parent: NodeAddress): (Map[String, BsonValue], Vector[DatabaseMutactionResult]) = {
    val (childResults: Vector[DatabaseMutactionResult], grouped: Map[RelationField, immutable.Seq[Document]]) =
      nestedCreateDocAndResultHelper(mutaction, parent)
    val nestedCreateFields = grouped.foldLeft(Map.empty[String, BsonValue]) { (map, t) =>
      val rf: RelationField = t._1
      val documents         = t._2.map(_.toBsonDocument)

      if (rf.isList) map + (rf.dbName -> BsonArray(documents)) else map + (rf.dbName -> documents.head)
    }

    (nestedCreateFields, childResults)
  }

  private def embeddedNestedCreateDocsAndResultsThatCanBeWithinUpdate(
      mutaction: FurtherNestedMutaction,
      parent: NodeAddress): (Map[RelationField, Vector[BsonDocument]], Vector[DatabaseMutactionResult]) = {
    val (childResults, grouped) = nestedCreateDocAndResultHelper(mutaction, parent)
    val nestedCreateFields      = grouped.map { case (f, v) => (f, v.map(_.toBsonDocument).toVector) }

    (nestedCreateFields, childResults)
  }

  private def nestedCreateDocAndResultHelper(mutaction: FurtherNestedMutaction, parent: NodeAddress) = {

    val nestedCreates = mutaction.nestedCreates.collect {
      case m if m.relationField.relatedModel_!.isEmbedded => m.relationField -> createToDoc(List.empty, m, Some(parent), Some(m.relationField))
    }
    val childResults: Vector[DatabaseMutactionResult]        = nestedCreates.flatMap(x => x._2._2)
    val grouped: Map[RelationField, immutable.Seq[Document]] = nestedCreates.groupBy(_._1).mapValues(_.map(_._2._1))
    (childResults, grouped)
  }

  private def embeddedNestedDeleteActionsAndResults(node: PrismaNode, mutaction: UpdateNode, parent: NodeAddress): (Vector[Bson], Vector[DeleteNodeResult]) = {
    val parentWhere = mutaction match {
      case top: TopLevelUpdateNode  => Some(top.where)
      case nested: NestedUpdateNode => nested.where
    }

    val actionsAndResults = mutaction.nestedDeletes.collect {
      case toOneDelete @ NestedDeleteNode(_, rf, None) if rf.relatedModel_!.isEmbedded =>
        node.getToOneChild(rf) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, parentWhere, toOneDelete.model, None)
          case Some(nestedNode) => (unset(parent.path.stringForField(rf.dbName)), DeleteNodeResult(nestedNode, toOneDelete))

        }

      case toManyDelete @ NestedDeleteNode(_, rf, Some(where)) if rf.relatedModel_!.isEmbedded =>
        node.getToManyChild(rf, where) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, parentWhere, toManyDelete.model, Some(where))
          case Some(nestedNode) => (pull(parent.path.stringForField(rf.dbName), whereToBson(where)), DeleteNodeResult(nestedNode, toManyDelete))
        }
    }

    (actionsAndResults.map(_._1), actionsAndResults.map(_._2))
  }

  private def scalarUpdateValues(mutaction: AllUpdateNodes, parent: NodeAddress): Vector[Bson] = {
    val model          = mutaction.model
    val invalidUpdates = mutaction.nonListArgs.raw.asRoot.map.collect { case (k, v) if v == NullGCValue && model.getFieldByName_!(k).isRequired => k }
    if (invalidUpdates.nonEmpty) throw FieldCannotBeNull(invalidUpdates.head)

    val nonListValues = mutaction.nonListArgs.raw.asRoot.map.map {
      case (f, v) => set(parent.path.stringForField(f, model), GCToBson(v))
    }.toVector
    val listValues = mutaction.listArgs.map { case (f, v) => set(parent.path.stringForField(f, model), GCToBson(v)) }

    nonListValues ++ listValues
  }

  private def embeddedNestedUpdateDocsAndResults(node: PrismaNode,
                                                 mutactions: Vector[UpdateNode],
                                                 parent: NodeAddress): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {

    val actionsArrayFiltersAndResults = mutactions.collect {
      case toOneUpdate @ NestedUpdateNode(_, rf, None, _, _, _, _, _, _, _, _, _, _, _) if rf.relatedModel_!.isEmbedded =>
        val updatedParent = parent.appendPath(rf)
        val subNode = node.getToOneChild(rf) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, None, rf.relatedModel_!, None)
          case Some(prismaNode) => prismaNode
        }

        val scalars                                = scalarUpdateValues(toOneUpdate, updatedParent)
        val (creates, createResults)               = embeddedNestedCreateActionsAndResults(toOneUpdate, updatedParent)
        val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(subNode, toOneUpdate.nestedUpdates, updatedParent)
        val (deletes, deleteResults)               = embeddedNestedDeleteActionsAndResults(subNode, toOneUpdate, updatedParent)
        val thisResult                             = UpdateNodeResult(subNode.id, subNode, toOneUpdate)

        (scalars ++ creates ++ deletes ++ updates, arrayFilters, createResults ++ deleteResults ++ updateResults :+ thisResult)

      case toManyUpdate @ NestedUpdateNode(_, rf, Some(where), _, _, _, _, _, _, _, _, _, _, _) if rf.relatedModel_!.isEmbedded =>
        val subNode = node.getToManyChild(rf, where) match {
          case None             => throw NodesNotConnectedError(rf.relation, rf.model, None, rf.relatedModel_!, Some(where))
          case Some(prismaNode) => prismaNode
        }
        val updatedParent = parent.appendPath(rf, where, subNode)

        val scalars                                      = scalarUpdateValues(toManyUpdate, updatedParent)
        val (creates, createResults)                     = embeddedNestedCreateActionsAndResults(toManyUpdate, updatedParent)
        val (updates, nestedArrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(subNode, toManyUpdate.nestedUpdates, updatedParent)
        val (deletes, deleteResults)                     = embeddedNestedDeleteActionsAndResults(subNode, toManyUpdate, updatedParent)

        val thisResult = UpdateNodeResult(updatedParent, subNode, toManyUpdate)

        (scalars ++ creates ++ deletes ++ updates,
         ArrayFilter.arrayFilter(updatedParent.path) ++ nestedArrayFilters,
         createResults ++ deleteResults ++ updateResults :+ thisResult)
    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  private def embeddedNestedUpdateManyDocsAndResults(node: PrismaNode,
                                                     mutactions: Vector[NestedUpdateNodes],
                                                     parent: NodeAddress): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {
    val actionsArrayFiltersAndResults = mutactions.collect {
      case updateNodes @ NestedUpdateNodes(_, _, rf, whereFilter, _, _) if rf.relatedModel_!.isEmbedded =>
        val updatedParent = parent.appendPath(rf, whereFilter)
        val scalars       = scalarUpdateValues(updateNodes, updatedParent)

        val thisResult = ManyNodesResult(updateNodes, 0)

        (scalars, ArrayFilter.arrayFilter(updatedParent.path), Vector(thisResult))
    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  private def embeddedNestedDeleteManyDocsAndResults(node: PrismaNode,
                                                     mutactions: Vector[NestedDeleteNodes],
                                                     parent: NodeAddress): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {
    val actionsArrayFiltersAndResults = mutactions.collect {
      case deleteNodes @ NestedDeleteNodes(_, _, rf, whereFilter) if rf.relatedModel_!.isEmbedded =>
        val deletes    = pull(parent.path.stringForField(rf.dbName), buildConditionForScalarFilter("", whereFilter))
        val thisResult = ManyNodesResult(deleteNodes, 0)

        (Vector(deletes), Vector.empty, Vector(thisResult))
    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  private def embeddedNestedUpsertDocsAndResults(node: PrismaNode,
                                                 mutaction: UpdateNode,
                                                 parent: NodeAddress): (Vector[Bson], Vector[Bson], Vector[DatabaseMutactionResult]) = {
    val actionsArrayFiltersAndResults = mutaction.nestedUpserts.collect {
      case toOneUpsert @ NestedUpsertNode(_, rf, None, create, update) if rf.relatedModel_!.isEmbedded =>
        node.getToOneChild(rf) match {
          case None =>
            val (createDoc, createResults) = createToDoc(List.empty, create, Some(parent), Some(rf))
            (Vector(push(rf.dbName, createDoc)), Vector.empty, createResults :+ UpsertNodeResult(toOneUpsert.create, toOneUpsert))

          case Some(_) =>
            val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(node, Vector(update), parent)
            (updates, arrayFilters, updateResults :+ UpsertNodeResult(toOneUpsert.update, toOneUpsert))
        }

      case toManyUpsert @ NestedUpsertNode(_, rf, Some(where), create, update) if rf.relatedModel_!.isEmbedded =>
        node.getToManyChild(rf, where) match {
          case None =>
            val (createDoc, createResults) = createToDoc(List.empty, create, Some(parent), Some(rf))
            (Vector(push(rf.dbName, createDoc)), Vector.empty, createResults :+ UpsertNodeResult(toManyUpsert.create, toManyUpsert))

          case Some(_) =>
            val (updates, arrayFilters, updateResults) = embeddedNestedUpdateDocsAndResults(node, Vector(update), parent)
            (updates, arrayFilters, updateResults :+ UpsertNodeResult(toManyUpsert.update, toManyUpsert))

        }

    }
    (actionsArrayFiltersAndResults.flatMap(_._1), actionsArrayFiltersAndResults.flatMap(_._2), actionsArrayFiltersAndResults.flatMap(_._3))
  }

  private def embeddedNestedCreateActionsAndResults(mutaction: FurtherNestedMutaction, parent: NodeAddress): (Vector[Bson], Vector[DatabaseMutactionResult]) = {
    mutaction match {
      case x: CreateNode =>
        val (nestedCreateFields, nestedCreateResults) = embeddedNestedCreateDocsAndResults(mutaction, parent)
        val nestedCreates                             = nestedCreateFields.map { case (f, v) => set(parent.path.stringForField(f, x.model), v) }.toVector
        (nestedCreates, nestedCreateResults)

      case x: UpdateNode =>
        val (nestedCreateFields: Map[RelationField, Vector[BsonDocument]], nestedCreateResults) =
          embeddedNestedCreateDocsAndResultsThatCanBeWithinUpdate(mutaction, parent)
        val nestedCreates = nestedCreateFields.collect {
          case (f, v) if !f.isList => set(parent.path.stringForField(f.dbName), v.head)
          case (f, v) if f.isList  => pushEach(parent.path.stringForField(f.dbName), v: _*)
        }.toVector

        (nestedCreates, nestedCreateResults)
    }
  }
}
