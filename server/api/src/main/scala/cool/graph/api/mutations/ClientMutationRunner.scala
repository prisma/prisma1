package cool.graph.api.mutations

import cool.graph.api.database.DataItem
import cool.graph.api.schema.{APIErrors, ApiUserContext, GeneralError}
import cool.graph.shared.models.Project

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ClientMutationRunner {

  def run(
      clientMutation: ClientMutation,
      requestContext: Option[ApiUserContext] = None,
      project: Project
  ): Future[DataItem] = {

    for {
      mutactionGroups  <- clientMutation.prepareMutactions()
      errors           <- clientMutation.verifyMutactions(mutactionGroups)
      _                = if (errors.nonEmpty) throw errors.head
      executionResults <- clientMutation.performMutactions(mutactionGroups)
      _                <- clientMutation.performPostExecutions(mutactionGroups)
      dataItem <- {
        executionResults
          .filter(_.isInstanceOf[GeneralError])
          .map(_.asInstanceOf[GeneralError]) match {
          case errors if errors.nonEmpty => throw errors.head
          case _ =>
            clientMutation.getReturnValue.map {
              case ReturnValue(dataItem) => dataItem
              case NoReturnValue(id)     => throw APIErrors.NodeNotFoundError(id)
            }
        }
      }
    } yield dataItem
  }
}
