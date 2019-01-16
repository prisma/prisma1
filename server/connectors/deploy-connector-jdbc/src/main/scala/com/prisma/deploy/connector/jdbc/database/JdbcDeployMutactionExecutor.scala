package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.jdbc.JdbcBase

import scala.concurrent.{ExecutionContext, Future}

case class JdbcDeployMutactionExecutor(builder: JdbcDeployDatabaseMutationBuilder)(implicit ec: ExecutionContext)
    extends JdbcBase
    with DeployMutactionExecutor {

  val slickDatabase = builder.slickDatabase

  override def execute(mutaction: DeployMutaction): Future[Unit] = {
    val action = mutaction match {
      case x: CreateProject         => CreateProjectInterpreter(builder).execute(x)
      case x: TruncateProject       => TruncateProjectInterpreter(builder).execute(x)
      case x: DeleteProject         => DeleteProjectInterpreter(builder).execute(x)
      case x: CreateColumn          => CreateColumnInterpreter(builder).execute(x)
      case x: UpdateColumn          => UpdateColumnInterpreter(builder).execute(x)
      case x: DeleteColumn          => DeleteColumnInterpreter(builder).execute(x)
      case x: CreateScalarListTable => CreateScalarListInterpreter(builder).execute(x)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter(builder).execute(x)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter(builder).execute(x)
      case x: CreateModelTable      => CreateModelInterpreter(builder).execute(x)
      case x: RenameTable           => RenameModelInterpreter(builder).execute(x)
      case x: DeleteModelTable      => DeleteModelInterpreter(builder).execute(x)
      case x: CreateRelationTable   => CreateRelationInterpreter(builder).execute(x)
      case x: UpdateRelationTable   => UpdateRelationInterpreter(builder).execute(x)
      case x: DeleteRelationTable   => DeleteRelationInterpreter(builder).execute(x)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter(builder).execute(x)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter(builder).execute(x)
    }

    if (slickDatabase.isSQLite) {
      import slickDatabase.profile.api._
      val list               = sql"""PRAGMA database_list;""".as[(String, String, String)]
      val path               = s"""'db/${mutaction.projectId}'"""
      val att                = sqlu"ATTACH DATABASE #${path} AS #${mutaction.projectId};"
      val activateForeignKey = sqlu"""PRAGMA foreign_keys = ON;"""

      val attach = for {
        attachedDbs <- list
        _ <- attachedDbs.map(_._2).contains(mutaction.projectId) match {
              case true  => DBIO.successful(())
              case false => att
            }
        _ <- activateForeignKey
      } yield ()
      database.run(DBIO.seq(attach, action).withPinnedSession).map(_ => ())
    } else {
      database.run(action).map(_ => ())
    }
  }

  override def rollback(mutaction: DeployMutaction): Future[Unit] = {
    val action = mutaction match {
      case x: CreateProject         => CreateProjectInterpreter(builder).rollback(x)
      case x: TruncateProject       => TruncateProjectInterpreter(builder).rollback(x)
      case x: DeleteProject         => DeleteProjectInterpreter(builder).rollback(x)
      case x: CreateColumn          => CreateColumnInterpreter(builder).rollback(x)
      case x: UpdateColumn          => UpdateColumnInterpreter(builder).rollback(x)
      case x: DeleteColumn          => DeleteColumnInterpreter(builder).rollback(x)
      case x: CreateScalarListTable => CreateScalarListInterpreter(builder).rollback(x)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter(builder).rollback(x)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter(builder).rollback(x)
      case x: CreateModelTable      => CreateModelInterpreter(builder).rollback(x)
      case x: RenameTable           => RenameModelInterpreter(builder).rollback(x)
      case x: DeleteModelTable      => DeleteModelInterpreter(builder).rollback(x)
      case x: CreateRelationTable   => CreateRelationInterpreter(builder).rollback(x)
      case x: UpdateRelationTable   => UpdateRelationInterpreter(builder).rollback(x)
      case x: DeleteRelationTable   => DeleteRelationInterpreter(builder).rollback(x)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter(builder).rollback(x)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter(builder).rollback(x)
    }

    if (slickDatabase.isSQLite) {
      import slickDatabase.profile.api._
      val list               = sql"""PRAGMA database_list;""".as[(String, String, String)]
      val path               = s"""'db/${mutaction.projectId}'"""
      val att                = sqlu"ATTACH DATABASE #${path} AS #${mutaction.projectId};"
      val activateForeignKey = sqlu"""PRAGMA foreign_keys = ON;"""

      val attach = for {
        attachedDbs <- list
        _ <- attachedDbs.map(_._2).contains(mutaction.projectId) match {
              case true  => DBIO.successful(())
              case false => att
            }
        _ <- activateForeignKey
      } yield ()
      database.run(DBIO.seq(attach, action).withPinnedSession).map(_ => ())
    } else {
      database.run(action).map(_ => ())
    }
  }
}
