package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder._
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.{NodeSelector, ParentInfo}
import com.prisma.shared.models.Project
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class NestedConnectRelationMutactions(project: Project, parentInfo: ParentInfo, where: NodeSelector, topIsCreate: Boolean)
    extends ClientSqlDataChangeMutaction {

  override def execute = {

    val topIsUpdate = !topIsCreate

    val p = parentInfo.field
    val c = parentInfo.relation.getOtherModel_!(project.schema, parentInfo.where.model).fields.find(_.relation.contains(parentInfo.relation)).get

    //these need to abort the transaction and return a proper error
    val checkForOldParent = oldParentFailureTrigger(project, parentInfo, where)
    val checkForOldChild  = oldChildFailureTrigger(project, parentInfo)

    val removalByParent         = deleteRelationRowByParent(project.id, parentInfo)
    val removalByChild          = deleteRelationRowByChild(project.id, parentInfo, where)
    val removalByParentAndChild = deleteRelationRowByParentAndChild(project.id, parentInfo, where)

    val requiredCheck =
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true) if topIsUpdate   => sys.error("ILLEGAL!")
        case (false, true, false, true) if topIsCreate   => sys.error("ILLEGAL!")
        case (false, true, false, false) if topIsUpdate  => List(checkForOldParent)
        case (false, true, false, false) if topIsCreate  => List(checkForOldParent)
        case (false, false, false, true) if topIsUpdate  => List.empty
        case (false, false, false, true) if topIsCreate  => List.empty
        case (false, false, false, false) if topIsUpdate => List.empty
        case (false, false, false, false) if topIsCreate => List.empty
        case (true, false, false, true) if topIsUpdate   => List.empty
        case (true, false, false, true) if topIsCreate   => List.empty
        case (true, false, false, false) if topIsUpdate  => List.empty
        case (true, false, false, false) if topIsCreate  => List.empty
        case (false, true, true, false) if topIsUpdate   => List.empty
        case (false, true, true, false) if topIsCreate   => List.empty
        case (false, false, true, false) if topIsUpdate  => List.empty
        case (false, false, true, false) if topIsCreate  => List.empty
        case (true, false, true, false) if topIsUpdate   => List.empty
        case (true, false, true, false) if topIsCreate   => List.empty

      }

    val removalActions =
      (p.isList, p.isRequired, c.isList, c.isRequired) match {
        case (false, true, false, true) if topIsUpdate   => sys.error("ILLEGAL!")
        case (false, true, false, true) if topIsCreate   => sys.error("ILLEGAL!")
        case (false, true, false, false) if topIsUpdate  => List.empty
        case (false, true, false, false) if topIsCreate  => List.empty
        case (false, false, false, true) if topIsUpdate  => List(removalByParent, removalByChild)
        case (false, false, false, true) if topIsCreate  => List(removalByChild)
        case (false, false, false, false) if topIsUpdate => List(removalByParent, removalByChild)
        case (false, false, false, false) if topIsCreate => List(removalByChild)
        case (true, false, false, true) if topIsUpdate   => List(removalByChild)
        case (true, false, false, true) if topIsCreate   => List(removalByChild)
        case (true, false, false, false) if topIsUpdate  => List(removalByChild)
        case (true, false, false, false) if topIsCreate  => List(removalByChild)
        case (false, true, true, false) if topIsUpdate   => List(removalByParent)
        case (false, true, true, false) if topIsCreate   => List.empty
        case (false, false, true, false) if topIsUpdate  => List(removalByParent)
        case (false, false, true, false) if topIsCreate  => List.empty
        case (true, false, true, false) if topIsUpdate   => List.empty
        case (true, false, true, false) if topIsCreate   => List.empty

      }

    val addAction = createRelationRowByUniqueValueForChild(project.id, parentInfo, where)

    val allActions = requiredCheck ++ removalActions :+ addAction

    Future.successful(ClientSqlStatementResult(DBIOAction.seq(allActions: _*)))
  }
}
