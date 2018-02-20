package com.prisma.api.database.mutactions.mutactions

import com.prisma.api.database.DatabaseMutationBuilder
import com.prisma.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import com.prisma.api.mutations.mutations.CascadingDeletes.Path
import com.prisma.api.mutations.{CoolArgs, NodeSelector, ParentInfo}
import com.prisma.shared.models.{Model, Project}
import slick.dbio.DBIOAction

import scala.concurrent.Future

case class UpdateDataItemByUniqueFieldIfInRelationWith(project: Project, path: Path, args: CoolArgs) extends ClientSqlDataChangeMutaction {

  val scalarArgs = args.nonListScalarArguments(path.lastModel)

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(DatabaseMutationBuilder.updateDataItemByPath(project.id, path, scalarArgs)))

}
