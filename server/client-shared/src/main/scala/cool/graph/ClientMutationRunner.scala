package cool.graph

import cool.graph.client.FeatureMetric
import cool.graph.client.mutactions._
import cool.graph.shared.errors.{GeneralError, UserAPIErrors}
import cool.graph.shared.models.{AuthenticatedRequest, Project}
import scaldi.Injector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ClientMutationRunner {
  def run(clientMutation: ClientMutation, authenticatedRequest: Option[AuthenticatedRequest], requestContext: RequestContextTrait, project: Project)(
      implicit inj: Injector): Future[DataItem] = {
    run(clientMutation, authenticatedRequest, Some(requestContext), project)
  }

  def run(clientMutation: ClientMutation,
          authenticatedRequest: Option[AuthenticatedRequest] = None,
          requestContext: Option[RequestContextTrait] = None,
          project: Project)(implicit inj: Injector): Future[DataItem] = {

    clientMutation.checkPermissions(authenticatedRequest) flatMap {
      case false => throw UserAPIErrors.InsufficientPermissions("Insufficient permissions for this mutation")

      case true =>
        for {
          mutactionGroups <- clientMutation.prepareMutactions()
          errors          <- clientMutation.verifyMutactions(mutactionGroups)
          _               = if (errors.nonEmpty) throw errors.head
          _ <- clientMutation
                .checkPermissionsAfterPreparingMutactions(authenticatedRequest, mutactionGroups.flatMap(_.mutactions flatMap {
                  case Transaction(clientSqlMutactions, _) => clientSqlMutactions
                  case x                                   => List(x)
                }))
          executionResults <- clientMutation.performMutactions(mutactionGroups)
          _                <- clientMutation.performPostExecutions(mutactionGroups)
          dataItem <- {
            trackApiMetrics(requestContext, mutactionGroups, project)

            requestContext.foreach(ctx => clientMutation.mutactionTimings.foreach(ctx.logMutactionTiming))

            executionResults
              .filter(_.isInstanceOf[GeneralError])
              .map(_.asInstanceOf[GeneralError]) match {
              case errors if errors.nonEmpty => throw errors.head
              case _ =>
                clientMutation.getReturnValue.map {
                  case ReturnValue(dataItem) => dataItem
                  case NoReturnValue(id)     => throw UserAPIErrors.NodeNotFoundError(id)
                }
            }
          }
        } yield dataItem
    }
  }

  private def trackApiMetrics(context: Option[RequestContextTrait], mutactionGroups: List[MutactionGroup], project: Project)(implicit inj: Injector): Unit = {

    def containsNestedMutation: Boolean = {
      val sqlMutactions = mutactionGroups.flatMap(_.mutactions collect { case Transaction(mutactions, _) => mutactions }).flatten

      val mutationMutactions = sqlMutactions.filter(m => m.isInstanceOf[CreateDataItem] || m.isInstanceOf[UpdateDataItem] || m.isInstanceOf[DeleteDataItem])

      mutationMutactions.length > 1
    }

    def containsServersideSubscriptions: Boolean =
      mutactionGroups.flatMap(_.mutactions.collect { case m: ServerSideSubscription => m }).nonEmpty

    context match {
      case Some(ctx) =>
        if (containsNestedMutation) {
          ctx.addFeatureMetric(FeatureMetric.NestedMutations)
        }
        if (containsServersideSubscriptions) {
          ctx.addFeatureMetric(FeatureMetric.ServersideSubscriptions)
        }
        Unit
      case _ => Unit
    }

  }
}
