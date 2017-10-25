package cool.graph

import cool.graph.Types.Id
import cool.graph.client.database.DataResolver
import cool.graph.cuid.Cuid
import cool.graph.shared.errors.{GeneralError, UserAPIErrors}
import cool.graph.shared.models.{AuthenticatedRequest, Model}
import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import cool.graph.utils.future.FutureUtils._
import sangria.schema.Args
import scaldi.Injector

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Try}

trait ClientMutationNew {
  def prepareMutactions(): Future[List[MutactionGroup]]

  def checkPermissions(authenticatedRequest: Option[AuthenticatedRequest]): Future[Boolean]

  def getReturnValue: Future[ReturnValueResult]
}

sealed trait ReturnValueResult
case class ReturnValue(dataItem: DataItem) extends ReturnValueResult
case class NoReturnValue(id: Id)           extends ReturnValueResult

abstract class ClientMutation(model: Model, args: Args, dataResolver: DataResolver, val argumentSchema: ArgumentSchema)(implicit inj: Injector)
    extends ClientMutationNew {

  dataResolver.enableMasterDatabaseOnlyMode

  var mutactionTimings: List[Timing] = List.empty

  val mutationId: Id = Cuid.createCuid()

  def prepareMutactions(): Future[List[MutactionGroup]]

  def prepareAndPerformMutactions(): Future[List[MutactionExecutionResult]] = {
    for {
      mutactionGroups <- prepareMutactions()
      results         <- performMutactions(mutactionGroups)
//      _               <- performPostExecutions(mutactionGroups) // this is probably not the way to go
    } yield results
  }

  def run(authenticatedRequest: Option[AuthenticatedRequest], requestContext: RequestContextTrait): Future[DataItem] = {
    run(authenticatedRequest, Some(requestContext))
  }

  def run(authenticatedRequest: Option[AuthenticatedRequest] = None, requestContext: Option[RequestContextTrait] = None): Future[DataItem] = {
    ClientMutationRunner.run(this, authenticatedRequest, requestContext, dataResolver.project)
  }

  def checkPermissions(authenticatedRequest: Option[AuthenticatedRequest]): Future[Boolean] = Future.successful(true)

  // Throw UserfacingError to reject
  def checkPermissionsAfterPreparingMutactions(authenticatedRequest: Option[AuthenticatedRequest], mutactions: List[Mutaction]): Future[Unit]

  val mutationDefinition: ClientMutationDefinition

  def performWithTiming[A](name: String, f: Future[A]): Future[A] = {
    val begin = System.currentTimeMillis()
    f andThen {
      case x =>
        mutactionTimings :+= Timing(name, System.currentTimeMillis() - begin)
        x
    }
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

  def extractScalarArgumentValues(args: Args): List[ArgumentValue] = {
    argumentSchema.extractArgumentValues(args, mutationDefinition.getSchemaArguments(model))
  }

  def extractIdFromScalarArgumentValues(args: Args, name: String): Option[Id] = {
    extractScalarArgumentValues(args).find(_.name == name).map(_.value.asInstanceOf[Id])
  }
  def extractIdFromScalarArgumentValues_!(args: Args, name: String): Id = {
    extractIdFromScalarArgumentValues(args, name).getOrElse(throw UserAPIErrors.IdIsMissing())
  }

  def performMutactions(mutactionGroups: List[MutactionGroup]): Future[List[MutactionExecutionResult]] = {

    def runWithErrorHandler(mutaction: Mutaction): Future[MutactionExecutionResult] = {
      mutaction.handleErrors match {
        case Some(errorHandler) => mutaction.execute.recover(errorHandler)
        case None               => mutaction.execute
      }
    }

    def performGroup(group: MutactionGroup): Future[List[MutactionExecutionResult]] = {
      group match {
        case MutactionGroup(mutactions, true) =>
          Future.sequence(mutactions.map(mutaction => performWithTiming(s"execute ${mutaction.getClass.getSimpleName}", runWithErrorHandler(mutaction))))
        case MutactionGroup(mutactions: List[Mutaction], false) =>
          mutactions.map(m => () => performWithTiming(s"execute ${m.getClass.getSimpleName}", runWithErrorHandler(m))).runSequentially
      }
    }

    // Cancel further Mutactions and MutactionGroups when a Mutaction fails
    // Failures in async MutactionGroups don't stop other Mutactions in same group
    mutactionGroups.map(group => () => performGroup(group)).runSequentially.map(_.flatten)
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
