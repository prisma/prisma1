package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.jdbc.JdbcBase

import scala.concurrent.{ExecutionContext, Future}

case class JdbcDeployMutactionExecutor(builder: JdbcDeployDatabaseMutationBuilder)(implicit ec: ExecutionContext)
    extends JdbcBase
    with DeployMutactionExecutor {

  val slickDatabase = builder.slickDatabase

  override def execute(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit] = {
    val action = mutaction match {
      case x: CreateProject         => CreateProjectInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: TruncateProject       => TruncateProjectInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteProject         => DeleteProjectInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateColumn          => CreateColumnInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateColumn          => UpdateColumnInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteColumn          => DeleteColumnInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateScalarListTable => CreateScalarListInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateModelTable      => CreateModelInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: RenameTable           => RenameModelInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteModelTable      => DeleteModelInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateRelationTable   => CreateRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateRelationTable   => UpdateRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteRelationTable   => DeleteRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter(builder).execute(x, schemaBeforeMigration)
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

  override def rollback(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit] = {
    val action = mutaction match {
      case x: CreateProject         => CreateProjectInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: TruncateProject       => TruncateProjectInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteProject         => DeleteProjectInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateColumn          => CreateColumnInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateColumn          => UpdateColumnInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteColumn          => DeleteColumnInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateScalarListTable => CreateScalarListInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateModelTable      => CreateModelInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: RenameTable           => RenameModelInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteModelTable      => DeleteModelInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateRelationTable   => CreateRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateRelationTable   => UpdateRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteRelationTable   => DeleteRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
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
