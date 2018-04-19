package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.DatabaseMutaction
import com.prisma.api.connector.mysql.MySqlApiConnector
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.postgresql.impls.DeployMutactionExecutorImpl
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils
import slick.dbio.DBIOAction

//import slick.jdbc.PostgresProfile.api._
//import slick.jdbc.PostgresProfile.backend.DatabaseDef

import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

case class ApiTestDatabase()(implicit dependencies: ApiDependencies) extends AwaitUtils {

  implicit lazy val system: ActorSystem             = dependencies.system
  implicit lazy val materializer: ActorMaterializer = dependencies.materializer
  private lazy val clientDatabase: DatabaseDef      = dependencies.apiConnector.asInstanceOf[MySqlApiConnector].databases.master

  def setup(project: Project): Unit = {
    deleteProjectDatabase(project)
    createProjectDatabase(project)

    // The order here is very important or foreign key constraints will fail
    project.models.foreach(createModelTable(project, _))
    project.relations.foreach(createRelationTable(project, _))
  }

  def truncateProjectTables(project: Project): Unit   = runMutaction(TruncateProject(project))
  def deleteProjectDatabase(project: Project): Unit   = runMutaction(DeleteProject(project.id))
  private def createProjectDatabase(project: Project) = runMutaction(CreateProject(project.id))

  private def createRelationTable(project: Project, relation: Relation) = runMutaction(CreateRelationTable(project.id, project.schema, relation = relation))

  def runMutaction(mutaction: DeployMutaction): Unit                            = DeployMutactionExecutorImpl(clientDatabase)(system.dispatcher).execute(mutaction).await
  def runDatabaseMutactionOnClientDb(mutaction: DatabaseMutaction)              = dependencies.databaseMutactionExecutor.execute(Vector(mutaction)).await()
  def runDbActionOnClientDb(action: DBIOAction[Any, NoStream, Effect.All]): Any = clientDatabase.run(action).await()

  private def createModelTable(project: Project, model: Model) = {
    runMutaction(CreateModelTable(project.id, model.name))

    model.scalarNonListFields
      .filter(f => !ReservedFields.reservedFieldNames.contains(f.name))
      .map(field => CreateColumn(project.id, model, field))
      .map(runMutaction)

    model.scalarListFields
      .map(field => CreateScalarListTable(project.id, model.name, field.name, field.typeIdentifier))
      .map(runMutaction)
  }
}
