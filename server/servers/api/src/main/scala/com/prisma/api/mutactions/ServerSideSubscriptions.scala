package com.prisma.api.mutactions

import com.prisma.api.connector._
import com.prisma.api.{ApiDependencies, ApiMetrics}
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models.{Model, ModelMutationType, Project, ServerSideSubscriptionFunction}
import com.prisma.subscriptions.schema.QueryTransformer
import sangria.parser.QueryParser

object ServerSideSubscriptions {
  def extractFromMutactions(
      project: Project,
      mutactions: Vector[DatabaseMutaction],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Vector[ServerSideSubscription] = {
    val createMutactions = mutactions.collect { case x: CreateDataItem => x }
    val updateMutactions = mutactions.collect { case x: UpdateDataItem => x }
    val deleteMutactions = mutactions.collect { case x: DeleteDataItem => x }

    val result = extractFromCreateMutactions(project, createMutactions, requestId) ++
      extractFromUpdateMutactions(project, updateMutactions, requestId) ++
      extractFromDeleteMutactions(project, deleteMutactions, requestId)
    ApiMetrics.subscriptionEventCounter.incBy(result.size, project.id)
    result
  }

  def extractFromCreateMutactions(
      project: Project,
      mutactions: Vector[CreateDataItem],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Vector[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- serverSideSubscriptionFunctionsFor(project, mutaction.model, ModelMutationType.Deleted)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.model,
        ModelMutationType.Created,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId
      )
    }
  }

  def extractFromUpdateMutactions(
      project: Project,
      mutactions: Vector[UpdateDataItem],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Vector[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- serverSideSubscriptionFunctionsFor(project, mutaction.model, ModelMutationType.Deleted)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.model,
        ModelMutationType.Updated,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId,
        updatedFields = Some(mutaction.namesOfUpdatedFields.toList),
        previousValues = Some(mutaction.previousValues.toDataItem)
      )
    }

  }

  def extractFromDeleteMutactions(
      project: Project,
      mutactions: Vector[DeleteDataItem],
      requestId: Id
  )(implicit apiDependencies: ApiDependencies): Vector[ServerSideSubscription] = {
    for {
      mutaction <- mutactions
      sssFn     <- serverSideSubscriptionFunctionsFor(project, mutaction.path.root.model, ModelMutationType.Deleted)
    } yield {
      ServerSideSubscription(
        project,
        mutaction.path.root.model,
        ModelMutationType.Deleted,
        sssFn,
        nodeId = mutaction.id,
        requestId = requestId,
        previousValues = Some(mutaction.previousValues)
      )
    }
  }

  private def serverSideSubscriptionFunctionsFor(project: Project, model: Model, mutationType: ModelMutationType) = {
    def isServerSideSubscriptionForModelAndMutationType(function: ServerSideSubscriptionFunction): Boolean = {
      val queryDoc             = QueryParser.parse(function.query).get
      val modelNameInQuery     = QueryTransformer.getModelNameFromSubscription(queryDoc).get
      val mutationTypesInQuery = QueryTransformer.getMutationTypesFromSubscription(queryDoc)
      model.name == modelNameInQuery && mutationTypesInQuery.contains(mutationType)
    }
    project.serverSideSubscriptionFunctions.filter(isServerSideSubscriptionForModelAndMutationType)
  }
}
