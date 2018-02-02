package com.prisma.api.database.mutactions.mutactions

import java.sql.SQLException

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.NodeSelector
import com.prisma.api.schema.APIErrors.RequiredRelationWouldBeViolated
import com.prisma.gc_values._
import com.prisma.shared.models.{Field, Project, Relation}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class DeleteRelationMutaction(project: Project, where: NodeSelector) extends ClientSqlDataChangeMutaction {

  val relationsWhereThisIsRequired    = where.model.relationFields.filter(otherSideIsRequired).map(_.relation.get)
  val relationsWhereThisIsNotRequired = where.model.relationFields.filter(otherSideIsNotRequired).map(_.relation.get)

  val requiredCheck =
    relationsWhereThisIsRequired.map(relation => DatabaseMutationBuilder.oldParentFailureTriggerForRequiredRelations(project, relation, where))

  val removalActions = relationsWhereThisIsNotRequired.map(relation => DatabaseMutationBuilder.deleteRelationRowByChild(project.id, relation, where))

  override def execute = {
    val allActions = requiredCheck ++ removalActions
    Future.successful(ClientSqlStatementResult(DBIOAction.seq(allActions: _*)))
  }

  override def handleErrors = {
    Some({
      case e: SQLException if e.getErrorCode == 1242 && otherFailingRequiredRelationOnChild(e.getCause.toString).isDefined =>
        throw RequiredRelationWouldBeViolated(project, otherFailingRequiredRelationOnChild(e.getCause.toString).get)
    })
  }

  def otherFailingRequiredRelationOnChild(cause: String): Option[Relation] = relationsWhereThisIsRequired.collectFirst {
    case x if causedByThisMutactionChildOnly(x, cause) => x
  }

  def causedByThisMutactionChildOnly(relation: Relation, cause: String) = {
    val parentCheckString = s"`${project.id}`.`${relation.id}` WHERE `${relation.sideOf(where.model)}` = (SELECT"

    cause.contains(parentCheckString) && cause.contains(parameterString(where))
  }

  def parameterString(where: NodeSelector) = where.fieldValue match {
    case StringGCValue(x)      => s"parameters ['$x',"
    case IntGCValue(x)         => s"parameters [$x,"
    case FloatGCValue(x)       => s"parameters [$x,"
    case BooleanGCValue(false) => s"parameters [0,"
    case BooleanGCValue(true)  => s"parameters [1,"
    case GraphQLIdGCValue(x)   => s"parameters ['$x',"
    case EnumGCValue(x)        => s"parameters ['$x',"
    case DateTimeGCValue(x)    => throw sys.error("Implement DateTime") // todo
    case JsonGCValue(x)        => s"parameters ['$x',"
    case ListGCValue(_)        => sys.error("Not an acceptable Where")
    case RootGCValue(_)        => sys.error("Not an acceptable Where")
    case NullGCValue           => sys.error("Not an acceptable Where")
  }

  def otherSideIsRequired(field: Field): Boolean = field.relatedField(project.schema) match {
    case Some(f) if f.isRequired => true
    case Some(f)                 => false
    case None                    => false
  }

  def otherSideIsNotRequired(field: Field): Boolean = !otherSideIsRequired(field)

}
