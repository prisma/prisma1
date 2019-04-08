package com.prisma.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.connector.TopLevelDatabaseMutaction
import com.prisma.deploy.connector._
import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.ConnectorCapability.RelationLinkListCapability
import com.prisma.shared.models.Manifestations.EmbeddedRelationLink
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
  def deleteProjectDatabase(project: Project): Unit   = dependencies.deployConnector.deleteProjectDatabase(project.dbName).await()
  private def createProjectDatabase(project: Project) = dependencies.deployConnector.createProjectDatabase(project.dbName).await()

  //Fixme how does this work with self relations?
  private def createRelationTable(project: Project, relation: Relation) = {
    val mutaction = relation.manifestation match {
      case m: EmbeddedRelationLink if dependencies.deployConnector.capabilities.hasNot(RelationLinkListCapability) =>
        val modelA              = relation.modelA
        val modelB              = relation.modelB
        val (model, references) = if (m.inTableOfModelName == modelA.name) (modelA, modelB) else (modelB, modelA)

        CreateInlineRelation(project, relation, model, references, m.referencingColumn)
      case _ =>
        CreateRelationTable(project, relation = relation)
    }
    runMutaction(mutaction)
  }

  private def runMutaction(mutaction: DeployMutaction)                     = dependencies.deployConnector.deployMutactionExecutor.execute(mutaction, DatabaseSchema.empty).await
  def runDatabaseMutactionOnClientDb(mutaction: TopLevelDatabaseMutaction) = dependencies.databaseMutactionExecutor.executeTransactionally(mutaction).await

  private def createModelTable(project: Project, model: Model) = {
    runMutaction(CreateModelTable(project, model))

    model.scalarNonListFields
      .filter(f => f.name != ReservedFields.idFieldName)
      .map(field => CreateColumn(project, model, field))
      .foreach(runMutaction)

    model.scalarListFields
      .map(field => CreateScalarListTable(project, model, field))
      .foreach(runMutaction)
  }
}
