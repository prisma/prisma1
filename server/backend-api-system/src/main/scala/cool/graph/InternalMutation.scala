package cool.graph

import cool.graph.metrics.CustomTag
import cool.graph.shared.database.{InternalAndProjectDbs, InternalDatabase}
import cool.graph.shared.errors.CommonErrors.MutationsNotAllowedForProject
import cool.graph.shared.models.Project
import sangria.relay.Mutation
import scaldi.Injector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}

abstract class InternalProjectMutation[+ReturnValue <: Mutation] extends InternalMutation[ReturnValue] {

  val projectDbsFn: Project => InternalAndProjectDbs
  val project: Project

  override val databases: InternalAndProjectDbs   = projectDbsFn(project)
  override val internalDatabase: InternalDatabase = databases.internal

  override def verifyActions(): Future[List[Try[MutactionVerificationSuccess]]] = {
    if (actions.exists(_.isInstanceOf[ClientSqlMutaction]) && !project.allowMutations) {
      Future.failed(MutationsNotAllowedForProject(project.id))
    } else {
      super.verifyActions()
    }
  }
}

abstract class InternalMutation[+ReturnValue <: Mutation] {
  import InternalMutationMetrics._

  val internalDatabase: InternalDatabase
  val databases: InternalAndProjectDbs = InternalAndProjectDbs(internal = internalDatabase)

  def trusted(input: TrustedInternalMutationInput[Product])(implicit inj: Injector): TrustedInternalMutation[ReturnValue] = {
    TrustedInternalMutation(this, input, this.internalDatabase)
  }

  val args: Product
  var actions: List[Mutaction]                                           = List.empty[Mutaction]
  var actionVerificationResults: List[Try[MutactionVerificationSuccess]] = List.empty[Try[MutactionVerificationSuccess]]
  var actionExecutionResults: List[MutactionExecutionResult]             = List.empty[MutactionExecutionResult]
  var mutactionTimings: List[Timing]                                     = List.empty

  def prepareActions(): List[Mutaction]

  def verifyActions(): Future[List[Try[MutactionVerificationSuccess]]] = {
    val mutactionFutures = actions.map { action =>
      require(!action.isInstanceOf[ClientSqlDataChangeMutaction], "This must not be called with ClientSqlDataChangeMutatctions")
      InternalMutation.performWithTiming(s"verify ${action.getClass.getSimpleName}", action.verify(), timing => mutactionTimings :+= timing)
    }

    Future
      .sequence(mutactionFutures)
      .andThen {
        case Success(res) => actionVerificationResults = res
      }
  }

  def performActions(requestContext: Option[SystemRequestContextTrait] = None): Future[List[MutactionExecutionResult]] = {
    runningMutactionsCounter.incBy(actions.size)
    new InternalMutactionRunner(requestContext, databases, timing => mutactionTimings :+= timing).run(this, actions)
  }

  def getReturnValue: Option[ReturnValue]

  def run(requestContext: SystemRequestContextTrait): Future[ReturnValue] = run(Some(requestContext))

  def run(requestContext: Option[SystemRequestContextTrait] = None): Future[ReturnValue] = {
    runningMutationsCounter.inc()
    def performAndLog = {
      for {
        mutactionResults <- performActions(requestContext)
        _                = logTimings(mutactionResults)
      } yield getReturnValue.get
    }

    def logTimings(results: List[MutactionExecutionResult]): Unit = {
      requestContext.foreach(ctx => mutactionTimings.foreach(ctx.logMutactionTiming))
    }

    prepareActions()

    mutationTimer.timeFuture(customTagValues = this.getClass.getSimpleName) {
      for {
        verifications <- verifyActions()
        firstError    = verifications.find(_.isFailure)
        result <- if (firstError.isDefined) {
                   throw firstError.get.failed.get
                 } else {
                   performAndLog
                 }
      } yield result
    }
  }
}

object InternalMutation {
  def performWithTiming[A](name: String, f: Future[A], log: Function[Timing, Unit]): Future[A] = {
    val begin = System.currentTimeMillis()
    f andThen {
      case x =>
        log(Timing(name, System.currentTimeMillis() - begin))
        x
    }
  }
}

object InternalMutationMetrics {
  import cool.graph.system.metrics.SystemMetrics._

  val runningMutationsCounter  = defineCounter("runningMutations")
  val runningMutactionsCounter = defineCounter("runningMutactions")

  val mutationTimer  = defineTimer("mutationTime", CustomTag("name", recordingThreshold = 1000))
  val mutactionTimer = defineTimer("mutactionTime", CustomTag("name", recordingThreshold = 500))
}
