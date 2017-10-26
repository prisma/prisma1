package cool.graph.client.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.authorization.{ModelPermissions, PermissionValidator, RelationMutationPermissions}
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.mutations.definitions.CreateDefinition
import cool.graph.client.requestPipeline.RequestPipelineRunner
import cool.graph.client.schema.InputTypesBuilder
import cool.graph.cuid.Cuid
import cool.graph.shared.models._
import sangria.schema
import scaldi.{Injectable, Injector}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Create(model: Model,
             project: Project,
             args: schema.Args,
             dataResolver: DataResolver,
             argumentSchema: ArgumentSchema,
             allowSettingManagedFields: Boolean = false)(implicit inj: Injector)
    extends ClientMutation(model, args, dataResolver, argumentSchema)
    with Injectable {

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")

  override val mutationDefinition = CreateDefinition(argumentSchema, project, InputTypesBuilder(project, argumentSchema))

  val permissionValidator: PermissionValidator = new PermissionValidator(project)
  val id: Id                                   = Cuid.createCuid()
  val requestId: String                        = dataResolver.requestContext.map(_.requestId).getOrElse("")
  val pipelineRunner                           = new RequestPipelineRunner(requestId)

  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("input") match { // TODO: input token is probably relay specific?
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }

    CoolArgs(argsPointer, argumentSchema, model, project)
  }

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    val createMutactionsResult =
      SqlMutactions(dataResolver).getMutactionsForCreate(project, model, coolArgs, allowSettingManagedFields, id, requestId = requestId)

    val transactionMutaction       = Transaction(createMutactionsResult.allMutactions, dataResolver)
    val algoliaSyncQueryMutactions = AlgoliaSyncQueries.extract(dataResolver, project, model, id, "create")
    val createMutactions           = createMutactionsResult.allMutactions.collect { case x: CreateDataItem => x }

    val actionMutactions = ActionWebhooks.extractFromCreateMutactions(
      project = project,
      mutactions = createMutactions,
      mutationId = mutationId,
      requestId = requestId
    )

    val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, createMutactionsResult.allMutactions)
    val sssActions             = ServerSideSubscription.extractFromMutactions(project, createMutactionsResult.allMutactions, requestId)

    Future.successful(
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false),
        MutactionGroup(mutactions = actionMutactions.toList ++ sssActions ++ algoliaSyncQueryMutactions ++ subscriptionMutactions, async = true)
      ))

  }

  override def checkPermissions(authenticatedRequest: Option[AuthenticatedRequest]): Future[Boolean] = {
    val normalPermissions = ModelPermissions.checkPermissionsForCreate(model, coolArgs, authenticatedRequest, project)

    def checkCustomPermissionsForField(field: Field): Future[Boolean] = {
      val filteredPermissions = model.permissions
        .filter(_.isActive)
        .filter(_.operation == ModelOperation.Create)
        .filter(p => p.applyToWholeModel || p.fieldIds.contains(field.id))

      permissionValidator.checkModelQueryPermissions(
        project,
        filteredPermissions,
        authenticatedRequest,
        "not-the-id",
        coolArgs.permissionQueryArgsForNewFieldValues(mutationDefinition),
        alwaysQueryMasterDatabase = true
      )
    }
    if (normalPermissions) {
      Future.successful(true)
    } else {
      Future
        .sequence(coolArgs.fieldsThatRequirePermissionCheckingInMutations.map(checkCustomPermissionsForField))
        .map { x =>
          x.nonEmpty && x.forall(identity)
        }
    }
  }

  override def checkPermissionsAfterPreparingMutactions(authenticatedRequest: Option[AuthenticatedRequest], mutactions: List[Mutaction]): Future[Unit] = {
    RelationMutationPermissions.checkAllPermissions(project, mutactions, authenticatedRequest)
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    for {
      returnValue <- returnValueById(model, id)
      dataItem    = returnValue.asInstanceOf[ReturnValue].dataItem
      transformedResult <- pipelineRunner.runTransformPayload(project = project,
                                                              model = model,
                                                              operation = RequestPipelineOperation.CREATE,
                                                              values = RequestPipelineRunner.dataItemToArgumentValues(dataItem, model))
    } yield {
      ReturnValue(RequestPipelineRunner.argumentValuesToDataItem(transformedResult, dataItem.id, model))
    }
  }
}
