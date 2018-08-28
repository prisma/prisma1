package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.FieldCombinators._
import com.prisma.api.connector.mongo.extensions.DocumentToRoot
import com.prisma.api.connector.mongo.extensions.GCBisonTransformer.GCValueBsonTransformer
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.WhereToBson
import com.prisma.api.schema.APIErrors
import com.prisma.api.schema.APIErrors.{FieldCannotBeNull, NodesNotConnectedError}
import com.prisma.gc_values._
import com.prisma.shared.models.RelationField
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonArray, BsonValue, conversions}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.{Document, MongoCollection}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

trait NodeActions extends NodeSingleQueries {

  def createToDoc(mutaction: CreateNode, results: Vector[DatabaseMutactionResult] = Vector.empty): (Document, Vector[DatabaseMutactionResult]) = {
    val nonListValues: List[(String, GCValue)] =
      mutaction.model.scalarNonListFields
        .filter(field => mutaction.nonListArgs.hasArgFor(field) && mutaction.nonListArgs.getFieldValue(field.name).get != NullGCValue)
        .map(field => field.name -> mutaction.nonListArgs.getFieldValue(field).get)

    val (childResults: Vector[DatabaseMutactionResult], nestedCreateFields: Map[String, BsonValue]) = nestedCreateDocsAndResults(mutaction.nestedCreates)

    val doc = Document(nonListValues ++ mutaction.listArgs) ++ nestedCreateFields

    (doc, childResults)
  }

  private def nestedCreateDocsAndResults(mutactions: Vector[NestedCreateNode]) = {
    val nestedCreates: immutable.Seq[(RelationField, (Document, Vector[DatabaseMutactionResult]))] =
      mutactions.map(m => m.relationField -> createToDoc(m))

    val childResults = nestedCreates.flatMap(x => x._2._2).toVector

    val grouped: Map[RelationField, immutable.Seq[Document]] = nestedCreates.groupBy(_._1).mapValues(_.map(_._2._1))

    val nestedCreateFields = grouped.foldLeft(Map.empty[String, BsonValue]) { (map, t) =>
      val rf: RelationField = t._1
      val documents         = t._2.map(_.toBsonDocument)

      if (rf.isList) {
        map + (rf.name -> BsonArray(documents))
      } else {
        map + (rf.name -> documents.head)
      }
    }
    (childResults, nestedCreateFields)
  }

  def createNode(mutaction: CreateNode, includeRelayRow: Boolean)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection: MongoCollection[Document] = database.getCollection(mutaction.model.dbName)
      val id                                    = CuidGCValue.random()

      val (docWithoutId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(mutaction)
      val docWithId                                                               = Document(docWithoutId.toMap + ("_id" -> GCValueBsonTransformer(id)))

      collection.insertOne(docWithId).toFuture().map(_ => MutactionResults(Vector(CreateNodeResult(id, mutaction)) ++ childResults))
    }

  def createNestedNode(mutaction: NestedCreateNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
//      val parentModel                                                             = mutaction.relationField.model
//      val relatedField                                                            = mutaction.relationField
//      val filter                                                                  = WhereToBson(NodeSelector.forIdGCValue(parentModel, parentId))
//      val collection: MongoCollection[Document]                                   = database.getCollection(parentModel.dbName)
//      val (docWithoutId: Document, childResults: Vector[DatabaseMutactionResult]) = createToDoc(mutaction)
//      val updates                                                                 = set(relatedField.name, docWithoutId)
//
//      collection
//        .updateOne(filter, updates)
//        .toFuture()
//        .map(_ => MutactionResults(Vector(CreateNodeResult(CuidGCValue.random(), mutaction)) ++ childResults))

      ???
    }

  def deleteNode(mutaction: TopLevelDeleteNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document] = database.getCollection(mutaction.model.name)
    val filter                                = WhereToBson(mutaction.where)

    val previousValues: Future[Option[PrismaNode]] = collection.find(filter).collect().toFuture.map { results: Seq[Document] =>
      results.headOption.map { result =>
        val root = DocumentToRoot(mutaction.model, result)
        PrismaNode(root.idField, root)
      }
    }

    previousValues.flatMap {
      case Some(node) => collection.deleteOne(filter).toFuture().map(_ => MutactionResults(Vector(DeleteNodeResult(node.id, node, mutaction))))
      case None       => throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
    }
  }

  def deleteNodes(mutaction: DeleteNodes, shouldDeleteRelayIds: Boolean)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      val collection                        = database.getCollection(mutaction.model.name)
      val futureIds: Future[Seq[IdGCValue]] = getNodeIdsByFilter(mutaction.model, mutaction.whereFilter, database)

      futureIds.flatMap { ids =>
        val filter = Filters.in("_id", ids.map(_.value): _*)
        collection.deleteMany(filter).toFuture().map(_ => MutactionResults(Vector.empty))
      }
    }

  def nestedDeleteNode(mutaction: NestedDeleteNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      ???
    }

  def updateNode(mutaction: TopLevelUpdateNode)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] = SimpleMongoAction { database =>
    val collection: MongoCollection[Document]      = database.getCollection(mutaction.model.name)
    val previousValues: Future[Option[PrismaNode]] = getNodeByWhere(mutaction.where, database)

    previousValues.flatMap {
      case None =>
        throw APIErrors.NodeNotFoundForWhereError(mutaction.where)

      case Some(node) =>
        //check nonListArgs for null updates to required fields
        val invalidUpdates = mutaction.nonListArgs.raw.asRoot.map.collect {
          case (k, v) if v == NullGCValue && mutaction.model.getFieldByName_!(k).isRequired => k
        }
        if (invalidUpdates.nonEmpty) throw FieldCannotBeNull(invalidUpdates.head)

        val nonListValues: Vector[Bson] = mutaction.nonListArgs.raw.asRoot.map.map { case (k, v) => set(k, GCValueBsonTransformer(v)) }.toVector
        val listValues: Vector[Bson]    = mutaction.listArgs.map { case (f, v) => set(f, GCValueBsonTransformer(v)) }

        //    create
        val (nestedCreateResults: Vector[DatabaseMutactionResult], nestedCreateDocs: Map[String, BsonValue]) =
          nestedCreateDocsAndResults(mutaction.nestedCreates)
        val nestedCreates: Vector[Bson] = nestedCreateDocs.map { case (f, v) => set(f, v) }.toVector

        val (nestedDeletes: Vector[Bson], nestedDeleteResults: Vector[DeleteNodeResult]) = nestedDelete(node, mutaction)

        //    update - only toOne
        val toOneUpdates = mutaction.nestedUpdates.collect { case m if !m.relationField.isList => m }
        val (nestedUpdateResults: Vector[DatabaseMutactionResult], nestedUpdates: Vector[Bson]) =
          nestedUpdateDocsAndResults(node, toOneUpdates)

        val updates = combine(nonListValues ++ listValues ++ nestedCreates ++ nestedDeletes ++ nestedUpdates: _*)
        val results = Vector(UpdateNodeResult(node.id, node, mutaction)) ++ nestedCreateResults ++ nestedDeleteResults ++ nestedUpdateResults

        collection
          .updateOne(WhereToBson(mutaction.where), updates)
          .toFuture()
          .map(_ => MutactionResults(results))
    }
  }

  private def nestedDelete(node: PrismaNode, mutaction: TopLevelUpdateNode) = {
    def manyError(delete: NestedDeleteNode, childWhere: NodeSelector) = error(delete, Some(childWhere))
    def error(delete: NestedDeleteNode, where: Option[NodeSelector] = None) =
      throw NodesNotConnectedError(delete.relationField.relation, delete.relationField.model, Some(mutaction.where), delete.model, where)

    //    delete - toOne
    val toOneDeletes = mutaction.nestedDeletes.collect { case m if !m.relationField.isList => m }
    toOneDeletes.foreach(delete => if (node.data.map(delete.relationField.name) == NullGCValue) error(delete))

    val nestedOneDeletes: Vector[Bson] = toOneDeletes.map(delete => unset(delete.relationField.name))
    val nestedOneDeleteResults = toOneDeletes.map { delete =>
      val random = CuidGCValue.random
      DeleteNodeResult(random, PrismaNode(random, node.data.map(delete.relationField.name).asRoot), delete)
    }

    //    delete - toMany
    val toManyDeletes = mutaction.nestedDeletes.collect { case m if m.relationField.isList => m }
    toManyDeletes.foreach { delete =>
      val where = delete.where.get

      node.data.map(delete.relationField.name) match {
        case NullGCValue         => manyError(delete, where)
        case ListGCValue(values) => if (!values.map(_.asRoot).exists(root => root.map(where.fieldName) == where.fieldGCValue)) manyError(delete, where)
        case x                   => println(x)
      }
    }

// Fixme this seems to be too complex for the mongo scala driver. Reactive mongo exposes a raw api that we could use
//    val nestedManyDeletes: Vector[Bson] = toManyDeletes.map { delete =>
//      val where = delete.where.get
//
//      pullByFilter(Filters.eq(s"${delete.relationField.name}.${where.fieldName}", where.fieldGCValue.value))
////      pull(delete.relationField.name, s"${delete.relationField.name}.${where.fieldName}", where.fieldGCValue.value))
//    }
//    val nestedManyDeleteResults = toManyDeletes.map { delete =>
//      val random = CuidGCValue.random
//      DeleteNodeResult(random, PrismaNode(random, node.data.map(delete.relationField.name).asRoot), delete)
//    }

//    (nestedOneDeletes ++ nestedManyDeletes, nestedOneDeleteResults)
    (nestedOneDeletes, nestedOneDeleteResults)
  }

  def nestedUpdateDocsAndResults(previousValues: PrismaNode,
                                 mutactions: Vector[NestedUpdateNode],
                                 path: String = ""): (Vector[DatabaseMutactionResult], Vector[conversions.Bson]) = {

    val first: Vector[(Vector[Bson], Vector[DatabaseMutactionResult])] = mutactions.map { mutaction =>
      val rFN = mutaction.relationField.name

      val invalidUpdates = mutaction.nonListArgs.raw.asRoot.map.collect {
        case (k, v) if v == NullGCValue && mutaction.model.getFieldByName_!(k).isRequired => k
      }
      if (invalidUpdates.nonEmpty) throw FieldCannotBeNull(invalidUpdates.head)

      val nonListValues = mutaction.nonListArgs.raw.asRoot.map.map { case (f, v) => set(combineThree(path, rFN, f), GCValueBsonTransformer(v)) }.toVector

      val listValues = mutaction.listArgs.map { case (f, v) => set(combineThree(path, rFN, f), GCValueBsonTransformer(v)) }

      //    create
      val (nestedCreateResults: Vector[DatabaseMutactionResult], nestedCreateFields: Map[String, BsonValue]) =
        nestedCreateDocsAndResults(mutaction.nestedCreates)
      val nestedCreates = nestedCreateFields.map { case (f, v) => set(combineThree(path, rFN, f), v) }

      //    delete - only toOne
      val toOneDeletes  = mutaction.nestedDeletes.collect { case m if !m.relationField.isList => m }
      val nestedDeletes = toOneDeletes.map(delete => unset(combineThree(path, rFN, delete.relationField.name)))
      val nestedDeleteResults = toOneDeletes.map { delete =>
        val random = CuidGCValue.random
        DeleteNodeResult(random, PrismaNode(random, previousValues.data.map(delete.relationField.name).asRoot), delete)
      }

      //    update - only toOne
      val toOneUpdates = mutaction.nestedUpdates.collect { case m if !m.relationField.isList => m }
      val (nestedUpdateResults: Vector[DatabaseMutactionResult], nestedUpdates: Vector[conversions.Bson]) =
        nestedUpdateDocsAndResults(previousValues, toOneUpdates, combineTwo(path, rFN))

      (nonListValues ++ listValues ++ nestedCreates ++ nestedDeletes ++ nestedUpdates, nestedCreateResults ++ nestedDeleteResults ++ nestedUpdateResults)
    }

    (first.flatMap(_._2), first.flatMap(_._1))
  }

  def nestedUpdateNode(mutaction: NestedUpdateNode, parentId: IdGCValue)(implicit ec: ExecutionContext): SimpleMongoAction[MutactionResults] =
    SimpleMongoAction { database =>
      // this should not be called for embedded types since it should be rolled into the TopLevelUpdate

      ???
    }
}
