package com.prisma.deploy.connector.jdbc.database

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.shared.models.Project
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class JdbcDeployMutactionExecutor(builder: JdbcDeployDatabaseMutationBuilder)(implicit ec: ExecutionContext)
    extends JdbcBase
    with DeployMutactionExecutor {

  val slickDatabase = builder.slickDatabase

  def runAttached[T](project: Project, action: DBIO[T]) = {
    if (slickDatabase.isSQLite) {
      import slickDatabase.profile.api._
      val list               = sql"""PRAGMA database_list;""".as[(String, String, String)]
      val path               = s"""'db/${project.dbName}'"""
      val att                = sqlu"ATTACH DATABASE #${path} AS #${project.dbName};"
      val activateForeignKey = sqlu"""PRAGMA foreign_keys = ON;"""

      val attach = for {
        attachedDbs <- list
        _ <- attachedDbs.map(_._2).contains(project.dbName) match {
              case true  => slick.dbio.DBIO.successful(())
              case false => att
            }
        _ <- activateForeignKey
      } yield ()
      database.run(slick.dbio.DBIO.seq(attach, action).withPinnedSession).map(_ => ())
    } else {
      database.run(action).map(_ => ())
    }
  }

  override def execute(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit] = {
    val action = mutaction match {
      case x: TruncateProject       => TruncateProjectInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateColumn          => CreateColumnInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateColumn          => UpdateColumnInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteColumn          => DeleteColumnInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateScalarListTable => CreateScalarListInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateModelTable      => CreateModelInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateModelTable      => UpdateModelInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteModelTable      => DeleteModelInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateRelationTable   => CreateRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateRelationTable   => UpdateRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteRelationTable   => DeleteRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter(builder).execute(x, schemaBeforeMigration)
      case x: UpdateInlineRelation  => UpdateInlineRelationInterpreter(builder).execute(x, schemaBeforeMigration)
    }

    runAttached(mutaction.project, action)
  }

  override def rollback(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema): Future[Unit] = {
    val action = mutaction match {
      case x: TruncateProject       => TruncateProjectInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateColumn          => CreateColumnInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateColumn          => UpdateColumnInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteColumn          => DeleteColumnInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateScalarListTable => CreateScalarListInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateScalarListTable => UpdateScalarListInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteScalarListTable => DeleteScalarListInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateModelTable      => CreateModelInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateModelTable      => UpdateModelInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteModelTable      => DeleteModelInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateRelationTable   => CreateRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateRelationTable   => UpdateRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteRelationTable   => DeleteRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: CreateInlineRelation  => CreateInlineRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: DeleteInlineRelation  => DeleteInlineRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
      case x: UpdateInlineRelation  => UpdateInlineRelationInterpreter(builder).rollback(x, schemaBeforeMigration)
    }

    runAttached(mutaction.project, action)
  }
}
