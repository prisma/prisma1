package com.prisma.api.mutactions

import com.prisma.api.ApiMetrics
import com.prisma.api.connector._
import com.prisma.shared.models.IdType.Id
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models.{Model, ModelMutationType, Project, ServerSideSubscriptionFunction}
import com.prisma.subscriptions.schema.QueryTransformer
import sangria.parser.QueryParser

object ServerSideSubscriptions {
  def extractFromMutactionResults(
      project: Project,
      mutactionResults: MutactionResults,
      requestId: Id
  ): Vector[ExecuteServerSideSubscription] = {
    val createResults    = mutactionResults.results.collect { case m: CreateNodeResult => m }
    val updateMutactions = mutactionResults.results.collect { case x: UpdateNodeResult => x }
    val deleteMutactions = mutactionResults.results.collect { case x: DeleteNodeResult => x }

    val result = extractFromCreateMutactions(project, createResults, requestId) ++
      extractFromUpdateMutactions(project, updateMutactions, requestId) ++
      extractFromDeleteMutactions(project, deleteMutactions, requestId)

    ApiMetrics.subscriptionEventCounter.incBy(result.size, project.id)
    result
  }

  private def extractFromCreateMutactions(
      project: Project,
      mutactionResults: Vector[CreateNodeResult],
      requestId: Id
  ): Vector[ExecuteServerSideSubscription] = {
    for {
      mutactionResult <- mutactionResults
      sssFn           <- serverSideSubscriptionFunctionsFor(project, mutactionResult.mutaction.model, ModelMutationType.Created)
    } yield {
      ExecuteServerSideSubscription(
        project,
        mutactionResult.mutaction.model,
        ModelMutationType.Created,
        sssFn,
        nodeId = mutactionResult.id,
        requestId = requestId
      )
    }
  }

  private def extractFromUpdateMutactions(
      project: Project,
      mutactionResults: Vector[UpdateNodeResult],
      requestId: Id
  ): Vector[ExecuteServerSideSubscription] = {
    for {
      mutactionResult <- mutactionResults
      sssFn           <- serverSideSubscriptionFunctionsFor(project, mutactionResult.mutaction.model, ModelMutationType.Updated)
    } yield {
      ExecuteServerSideSubscription(
        project,
        mutactionResult.mutaction.model,
        ModelMutationType.Updated,
        sssFn,
        nodeId = mutactionResult.id,
        requestId = requestId,
        updatedFields = Some(mutactionResult.namesOfUpdatedFields.toList),
        previousValues = Some(mutactionResult.previousValues)
      )
    }

  }

  private def extractFromDeleteMutactions(
      project: Project,
      mutactionResults: Vector[DeleteNodeResult],
      requestId: Id
  ): Vector[ExecuteServerSideSubscription] = {
    for {
      mutactionResult <- mutactionResults
      sssFn           <- serverSideSubscriptionFunctionsFor(project, mutactionResult.mutaction.model, ModelMutationType.Deleted)
    } yield {
      ExecuteServerSideSubscription(
        project,
        mutactionResult.mutaction.model,
        ModelMutationType.Deleted,
        sssFn,
        nodeId = mutactionResult.previousValues.id,
        requestId = requestId,
        previousValues = Some(mutactionResult.previousValues)
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
