package com.prisma.api.connector.mongo.database

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.extensions.NodeSelectorBsonTransformer.whereToBson
import com.prisma.api.schema.APIErrors.{NodesNotConnectedError, RequiredRelationWouldBeViolated}
import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models.RelationField
import org.mongodb.scala.Document
import sun.security.util.Cache.EqualByteArray

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait ValidationActions extends FilterConditionBuilder {

  def ensureThatNodeIsNotConnected(relationField: RelationField, id: IdGCValue)(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val model = relationField.model
      relationField.relation.manifestation match {
        case Some(m: InlineRelationManifestation) if m.inTableOfModelId == model.name =>
          val collection                             = database.getCollection(model.dbName)
          val futureResult: Future[Option[Document]] = collection.find(NodeSelector.forId(model, id)).collect().toFuture.map(_.headOption)
          futureResult.map(optionRes =>
            optionRes.foreach { res =>
              (relationField.isList, res.get(m.referencingColumn)) match {
                case (true, Some(bison)) if !bison.asArray().isEmpty => throw RequiredRelationWouldBeViolated(relationField.relation)
                case (false, Some(_))                                => throw RequiredRelationWouldBeViolated(relationField.relation)
                case (_, _)                                          => Future.successful(())
              }
          })

        case Some(m: InlineRelationManifestation) =>
          val relatedModel = relationField.relatedModel_!
          val collection   = database.getCollection(relatedModel.dbName)
          val mongoFilter = relationField.isList match {
            case true  => ScalarFilter(model.idField_!.copy(name = m.referencingColumn), Contains(id))
            case false => ScalarFilter(model.idField_!.copy(name = m.referencingColumn), Equals(id))
          }
          val res = collection.find(buildConditionForFilter(Some(mongoFilter))).collect().toFuture
          res.map(list => if (list.nonEmpty) throw RequiredRelationWouldBeViolated(relationField.relation))
        case _ => sys.error("should not happen ")
      }
    }

  def ensureThatNodesAreConnected(relationField: RelationField, childId: IdGCValue, parentId: IdGCValue)(implicit ec: ExecutionContext) = SimpleMongoAction {
    database =>
      val model = relationField.model
      relationField.relation.manifestation match {
        case Some(m: InlineRelationManifestation) if m.inTableOfModelId == model.name =>
          val collection                             = database.getCollection(model.dbName)
          val futureResult: Future[Option[Document]] = collection.find(NodeSelector.forId(model, parentId)).collect().toFuture.map(_.headOption)
          futureResult.map(optionRes =>
            optionRes.foreach { res =>
              (relationField.isList, res.get(m.referencingColumn)) match {
                case (true, Some(x)) if x.asArray().getValues.asScala.map(_.asString()).map(_.getValue).contains(childId.value.toString) =>
                  Future.successful(())
                case (false, Some(x)) if x.asString.getValue == childId.value => Future.successful(())
                case (_, _)                                                   => throw NodesNotConnectedError(relationField.relation, model, None, relationField.relatedModel_!, None)
              }
          })
        case Some(m: InlineRelationManifestation) =>
          val relatedModel = relationField.relatedModel_!
          val collection   = database.getCollection(relatedModel.dbName)

          val filter = relationField.isList match {
            case true  => ScalarFilter(relatedModel.idField_!.copy(name = m.referencingColumn), Contains(parentId))
            case false => ScalarFilter(relatedModel.idField_!.copy(name = m.referencingColumn), Equals(parentId))
          }

          val whereFilter = ScalarFilter(relatedModel.idField_!, Equals(childId))
          val mongoFilter = buildConditionForFilter(Some(AndFilter(Vector(filter, whereFilter))))
          val res         = collection.find(mongoFilter).collect().toFuture
          res.map(
            list =>
              if (list.isEmpty)
                throw NodesNotConnectedError(
                  relation = relationField.relation,
                  parent = relationField.model,
                  parentWhere = Some(NodeSelector.forId(relationField.model, parentId)),
                  child = relationField.relatedModel_!,
                  childWhere = None
              ))

        case _ => sys.error("should not happen ")
      }
  }

  def ensureThatParentIsConnected(
      relationField: RelationField,
      parentId: IdGCValue
  )(implicit ec: ExecutionContext) =
    SimpleMongoAction { database =>
      val parentModel = relationField.model
      val childModel  = relationField.relatedModel_!
      relationField.relation.manifestation match {
        case Some(m: InlineRelationManifestation) if m.inTableOfModelId == parentModel.name =>
          val collection                             = database.getCollection(parentModel.dbName)
          val futureResult: Future[Option[Document]] = collection.find(NodeSelector.forId(parentModel, parentId)).collect().toFuture.map(_.headOption)
          futureResult.map(optionRes =>
            optionRes.foreach { res =>
              res.get(m.referencingColumn) match {
                case None => throw NodesNotConnectedError(relationField.relation, parentModel, None, relationField.relatedModel_!, None)
                case _    => Future.successful(())
              }
          })
        case Some(m: InlineRelationManifestation) =>
          val collection = database.getCollection(childModel.dbName)
          val mongoFilter = relationField.isList match {
            case true  => ScalarFilter(childModel.idField_!.copy(name = m.referencingColumn), Contains(parentId))
            case false => ScalarFilter(childModel.idField_!.copy(name = m.referencingColumn), Equals(parentId))
          }
          val res = collection.find(buildConditionForFilter(Some(mongoFilter))).collect().toFuture
          res.map(
            list =>
              if (list.isEmpty)
                throw NodesNotConnectedError(
                  relation = relationField.relation,
                  parent = relationField.model,
                  parentWhere = Some(NodeSelector.forId(relationField.model, parentId)),
                  child = relationField.relatedModel_!,
                  childWhere = None
              ))

        case _ => sys.error("should not happen ")
      }
    }

  def errorIfNodesAreInRelation(parentIds: Vector[IdGCValue], otherField: RelationField)(implicit ec: ExecutionContext) = SimpleMongoAction { database =>
    val otherModel   = otherField.model
    val relatedModel = otherField.relatedModel_!

    otherField.relation.manifestation match {
      case Some(m: InlineRelationManifestation) if m.inTableOfModelId == otherModel.name =>
        val mongoFilter = otherField.isList match {
          case true  => ScalarListFilter(otherModel.idField_!.copy(name = m.referencingColumn, isList = true), ListContainsSome(parentIds))
          case false => ScalarFilter(otherModel.idField_!.copy(name = m.referencingColumn), In(parentIds))
        }
        val res = database.getCollection(otherModel.dbName).find(buildConditionForFilter(Some(mongoFilter))).collect().toFuture
        res.map(list => if (list.nonEmpty) throw RequiredRelationWouldBeViolated(otherField.relation))

      case Some(m: InlineRelationManifestation) if m.inTableOfModelId == relatedModel.name =>
        val filter = ScalarFilter(relatedModel.idField_!, In(parentIds))
        val res    = database.getCollection(relatedModel.dbName).find(buildConditionForFilter(Some(filter))).collect().toFuture
        res.map { list =>
          list.foreach { doc =>
            if (!otherField.relatedField.isList && doc.get(m.referencingColumn).isDefined) throw RequiredRelationWouldBeViolated(otherField.relation)
          }
        }

      case _ => sys.error("should not happen ")
    }
  }

  def errorIfNodeIsInRelation(nodeId: IdGCValue, otherField: RelationField)(implicit ec: ExecutionContext) = SimpleMongoAction { database =>
    val otherModel   = otherField.model
    val relatedModel = otherField.relatedModel_!

    otherField.relation.manifestation match {
      case Some(m: InlineRelationManifestation) if m.inTableOfModelId == otherModel.name =>
        val mongoFilter = otherField.isList match {
          case true  => ScalarFilter(otherModel.idField_!.copy(name = m.referencingColumn), Contains(nodeId))
          case false => ScalarFilter(otherModel.idField_!.copy(name = m.referencingColumn), Equals(nodeId))
        }
        val res = database.getCollection(otherModel.dbName).find(buildConditionForFilter(Some(mongoFilter))).collect().toFuture
        res.map(list => if (list.nonEmpty) throw RequiredRelationWouldBeViolated(otherField.relation))

      case Some(m: InlineRelationManifestation) if m.inTableOfModelId == relatedModel.name =>
        val filter = ScalarFilter(relatedModel.idField_!, Equals(nodeId))
        val res    = database.getCollection(relatedModel.dbName).find(buildConditionForFilter(Some(filter))).collect().toFuture
        res.map { list =>
          list.foreach { doc =>
            if (!otherField.relatedField.isList && doc.get(m.referencingColumn).isDefined) throw RequiredRelationWouldBeViolated(otherField.relation)
          }
        }
      case _ => sys.error("should not happen ")
    }
  }
}
