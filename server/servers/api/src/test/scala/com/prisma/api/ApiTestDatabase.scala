package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.TopLevelDatabaseMutaction
import com.prisma.deploy.connector._
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils

case class ApiTestDatabase()(implicit dependencies: TestApiDependencies) extends AwaitUtils {
  implicit lazy val system: ActorSystem             = dependencies.system
  implicit lazy val materializer: ActorMaterializer = dependencies.materializer

  def setup(project: Project): Unit = {
    deleteProjectDatabase(project)
    dependencies.invalidationTestKit.publish(Only(project.id), project.id)
    createProjectDatabase(project)

    // The order here is very important or foreign key constraints will fail
    project.models.foreach(createModelTable(project, _))
    project.relations.foreach(createRelationTable(project, _))
  }

  def truncateProjectTables(project: Project): Unit   = runMutaction(TruncateProject(project))
  def deleteProjectDatabase(project: Project): Unit   = runMutaction(DeleteProject(project.id))
  private def createProjectDatabase(project: Project) = runMutaction(CreateProject(project.id))

  private def createRelationTable(project: Project, relation: Relation) = {
    val mutaction = relation.manifestation match {
      case Some(m: InlineRelationManifestation) =>
        val modelA = relation.modelA
        val modelB = relation.modelB

        val (model, references) = if (m.inTableOfModelId == modelA.name) {
          (modelA, modelB)
        } else {
          (modelB, modelA)
        }
        val field = relation.getFieldOnModel(m.inTableOfModelId)
        CreateInlineRelation(project.id, model, field, references, m.referencingColumn)
      case _ =>
        CreateRelationTable(project.id, project.schema, relation = relation)
    }
    runMutaction(mutaction)
  }

  def runMutaction(mutaction: DeployMutaction)                             = dependencies.deployConnector.deployMutactionExecutor.execute(mutaction).await
  def runDatabaseMutactionOnClientDb(mutaction: TopLevelDatabaseMutaction) = dependencies.databaseMutactionExecutor.executeTransactionally(mutaction).await

  private def createModelTable(project: Project, model: Model) = {
    runMutaction(CreateModelTable(project.id, model))

    model.scalarNonListFields
      .filter(f => f.name != ReservedFields.idFieldName)
      .map(field => CreateColumn(project.id, model, field))
      .map(runMutaction)

    model.scalarListFields
      .map(field => CreateScalarListTable(project.id, model, field))
      .map(runMutaction)
  }
}
