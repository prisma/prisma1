package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder, SequenceAction, SimpleMongoAction}
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.ListGCValue
import com.prisma.shared.models.{Model, RelationField}
import org.mongodb.scala.MongoWriteException

import scala.concurrent.ExecutionContext

case class CreateNodeInterpreter(mutaction: CreateNode)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[MutactionResults] = {
    mutationBuilder.createNode(mutaction, List.empty)
  }

  override val errorMapper = {
    case e: MongoWriteException if e.getError.getCode == 11000 && MongoErrorMessageHelper.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, MongoErrorMessageHelper.getFieldOption(mutaction.model, e).get)
    case e: MongoWriteException if e.getError.getCode == 11000 && e.getMessage.contains("_id_") =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, s"Field name: ${mutaction.model.idField_!.name}")
  }
}

object MongoErrorMessageHelper {

  def indexNameHelper(collectionName: String, fieldName: String, unique: Boolean): String = {
    val shortenedName = fieldName.replaceAll("_", "x") substring (0, (125 - 25 - collectionName.length - 12).min(fieldName.length))

    unique match {
      case false => shortenedName + "_R"
      case true  => shortenedName + "_U"
    }
  }

  def getFieldOption(model: Model, e: MongoWriteException): Option[String] = {
    model.scalarFields.filter { field =>
      val constraintName = indexNameHelper(model.dbName, field.dbName, true)
      e.getMessage.contains(constraintName)
    } match {
      case x +: _ => Some("Field name = " + x.name)
      case _      => None
    }
  }
}

case class NestedCreateNodeInterpreter(mutaction: NestedCreateNode)(implicit val ec: ExecutionContext)
    extends NestedDatabaseMutactionInterpreter
    with NestedRelationInterpreterBase {
  override def relationField = mutaction.relationField
  val model                  = p.relatedModel_!

  override def mongoAction(mutationBuilder: MongoActionsBuilder, parent: NodeAddress) = {
    implicit val mb: MongoActionsBuilder = mutationBuilder

    for {
      _       <- SequenceAction(Vector(requiredCheck(parent), removalAction(parent)))
      results <- createNodeAndConnectToParent(mutaction.relationField, mutationBuilder, parent)
    } yield results
  }

  private def createNodeAndConnectToParent(
      relationField: RelationField,
      mutationBuilder: MongoActionsBuilder,
      parent: NodeAddress
  )(implicit ec: ExecutionContext): MongoAction[MutactionResults] = relationField.relatedField.relationIsInlinedInParent match {
    case true => // ID is stored on this Node
      val inlineRelation = c.isList match {
        case true  => List((relationField.relatedField.dbName, ListGCValue(Vector(parent.idValue))))
        case false => List((relationField.relatedField.dbName, parent.idValue))
      }

      for {
        mutactionResult <- mutationBuilder.createNode(mutaction, inlineRelation)
        id              = mutactionResult.results.find(_.mutaction == mutaction).get.asInstanceOf[CreateNodeResult].id
      } yield mutactionResult

    case false => // ID is stored on other node, we need to update the parent with the inline relation id after creating the child.
      for {
        mutactionResult <- mutationBuilder.createNode(mutaction, List.empty)
        id              = mutactionResult.results.find(_.mutaction == mutaction).get.asInstanceOf[CreateNodeResult].id
        _               <- mutationBuilder.createRelation(relationField, parent, id)
      } yield mutactionResult
  }

  def requiredCheck(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = mutaction.topIsCreate match {
    case false =>
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true)   => requiredRelationViolation
        case (false, true, false, false)  => noCheckRequired
        case (false, false, false, true)  => checkForOldChild(parent)
        case (false, false, false, false) => noCheckRequired
        case (true, false, false, true)   => noCheckRequired
        case (true, false, false, false)  => noCheckRequired
        case (false, true, true, false)   => noCheckRequired
        case (false, false, true, false)  => noCheckRequired
        case (true, false, true, false)   => noCheckRequired
        case _                            => errorBecauseManySideIsRequired
      }

    case true =>
      noCheckRequired
  }

  def removalAction(parent: NodeAddress)(implicit mutationBuilder: MongoActionsBuilder) = mutaction.topIsCreate match {
    case false =>
      (p.isList, c.isList) match {
        case (false, false) => removalByParent(parent)
        case (true, false)  => noActionRequired
        case (false, true)  => removalByParent(parent)
        case (true, true)   => noActionRequired
      }

    case true =>
      noActionRequired
  }

  override val errorMapper = {
    case e: MongoWriteException if e.getError.getCode == 11000 && MongoErrorMessageHelper.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, MongoErrorMessageHelper.getFieldOption(mutaction.model, e).get)
    case e: MongoWriteException if e.getError.getCode == 11000 && e.getMessage.contains("_id_") =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, s"Field name: ${mutaction.model.idField_!.name}")
  }
}
