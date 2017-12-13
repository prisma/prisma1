package cool.graph.api.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.mutactions._
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.schema.{ApiUserContext, GeneralError}
import cool.graph.cuid.Cuid
import cool.graph.gc_values.GCValue
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{AuthenticatedRequest, Model}
import cool.graph.util.gc_value.GCAnyConverter
import cool.graph.utils.future.FutureUtils._
import sangria.schema.Args

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Try}

trait ClientMutationNew {
  def prepareMutactions(): Future[List[MutactionGroup]]

  def getReturnValue: Future[ReturnValueResult]
}

sealed trait ReturnValueResult
case class ReturnValue(dataItem: DataItem) extends ReturnValueResult
case class NoReturnValue(id: Id)           extends ReturnValueResult

abstract class ClientMutation(model: Model, args: Args, dataResolver: DataResolver)(implicit apiDependencies: ApiDependencies) extends ClientMutationNew {
//  import cool.graph.metrics.ClientSharedMetrics._

//  var mutactionTimings: List[Timing] = List.empty

  val mutationId: Id = Cuid.createCuid()

  def prepareMutactions(): Future[List[MutactionGroup]]

  def prepareAndPerformMutactions(): Future[List[MutactionExecutionResult]] = {
    for {
      mutactionGroups <- prepareMutactions()
      results         <- performMutactions(mutactionGroups)
//      _               <- performPostExecutions(mutactionGroups) // this is probably not the way to go
    } yield results
  }

  def run(authenticatedRequestrequestContext: ApiUserContext): Future[DataItem] = {
    run(None, Some(authenticatedRequestrequestContext))
  }

  def run(authenticatedRequest: Option[AuthenticatedRequest] = None, requestContext: Option[ApiUserContext] = None): Future[DataItem] = {
    ClientMutationRunner.run(this, authenticatedRequest, requestContext, dataResolver.project)
  }

  def performWithTiming[A](name: String, f: Future[A]): Future[A] = {
//    val begin = System.currentTimeMillis()
//    f andThen {
//      case x =>
//        mutactionTimings :+= Timing(name, System.currentTimeMillis() - begin)
//        x
//    }

    f
  }

  def returnValueById(model: Model, id: Id): Future[ReturnValueResult] = {
    dataResolver.resolveByModelAndId(model, id).map {
      case Some(dataItem) => ReturnValue(dataItem)
      case None           => NoReturnValue(id)
    }
  }

  def verifyMutactions(mutactionGroups: List[MutactionGroup]): Future[List[GeneralError]] = {
    val mutactions = mutactionGroups.flatMap(_.mutactions)
    val verifications: Seq[Future[Try[MutactionVerificationSuccess]]] = mutactions.map { mutaction =>
      lazy val verifyCall = mutaction match {
        case mutaction: ClientSqlDataChangeMutaction => mutaction.verify(dataResolver)
        case mutaction                               => mutaction.verify()
      }
      performWithTiming(s"verify ${mutaction.getClass.getSimpleName}", verifyCall)
    }
    val sequenced: Future[Seq[Try[MutactionVerificationSuccess]]] = Future.sequence(verifications)
    val errors                                                    = sequenced.map(_.collect { case Failure(x: GeneralError) => x }.toList)

    errors
  }

  def performMutactions(mutactionGroups: List[MutactionGroup]): Future[List[MutactionExecutionResult]] = {
    // Cancel further Mutactions and MutactionGroups when a Mutaction fails
    // Failures in async MutactionGroups don't stop other Mutactions in same group
    mutactionGroups.map(group => () => performGroup(group)).runSequentially.map(_.flatten)
  }

  private def performGroup(group: MutactionGroup): Future[List[MutactionExecutionResult]] = {
    group match {
      case MutactionGroup(mutactions, true) =>
        Future.sequence(mutactions.map(runWithTiming))

      case MutactionGroup(mutactions: List[Mutaction], false) =>
        mutactions.map(m => () => runWithTiming(m)).runSequentially
    }
  }

  private def runWithTiming(mutaction: Mutaction): Future[MutactionExecutionResult] = {
    performWithTiming(
      s"execute ${mutaction.getClass.getSimpleName}", {
        mutaction match {
          case mut: ClientSqlDataChangeMutaction =>
//            sqlDataChangeMutactionTimer.timeFuture(dataResolver.project.id) {
            runWithErrorHandler(mut)
//            }
          case mut =>
            runWithErrorHandler(mut)
        }
      }
    )
  }

  private def runWithErrorHandler(mutaction: Mutaction): Future[MutactionExecutionResult] = {
    mutaction.handleErrors match {
      case Some(errorHandler) => mutaction.execute.recover(errorHandler)
      case None               => mutaction.execute
    }
  }

  def performPostExecutions(mutactionGroups: List[MutactionGroup]): Future[Boolean] = {
    def performGroup(group: MutactionGroup) = {
      group match {
        case MutactionGroup(mutactions, true) =>
          Future.sequence(mutactions.map(mutaction => performWithTiming(s"performPostExecution ${mutaction.getClass.getSimpleName}", mutaction.postExecute)))
        case MutactionGroup(mutactions: List[Mutaction], false) =>
          mutactions.map(m => () => performWithTiming(s"performPostExecution ${m.getClass.getSimpleName}", m.postExecute)).runSequentially
      }
    }

    val mutationGroupResults: Future[List[Boolean]] = Future.sequence(mutactionGroups.map(performGroup)).map(_.flatten)
    mutationGroupResults.map(_.forall(identity))
  }
}
