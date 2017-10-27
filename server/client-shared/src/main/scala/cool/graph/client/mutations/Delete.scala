package cool.graph.client.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.adapters.GraphcoolDataTypes
import cool.graph.client.authorization.{ModelPermissions, PermissionQueryArg, PermissionValidator, RelationMutationPermissions}
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.mutations.definitions.DeleteDefinition
import cool.graph.client.requestPipeline.RequestPipelineRunner
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.models.{Action => ModelAction, _}
import sangria.schema
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class Delete[ManyDataItemType](model: Model,
                               modelObjectTypes: SchemaModelObjectTypesBuilder[ManyDataItemType],
                               project: Project,
                               args: schema.Args,
                               dataResolver: DataResolver,
                               argumentSchema: ArgumentSchema)(implicit inj: Injector)
    extends ClientMutation(model, args, dataResolver, argumentSchema)
    with Injectable {

  override val mutationDefinition = DeleteDefinition(argumentSchema, project)

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")
  val permissionValidator                      = new PermissionValidator(project)

  val id: Id = extractIdFromScalarArgumentValues_!(args, "id")

  var deletedItem: Option[DataItem] = None
  val requestId: Id                 = dataResolver.requestContext.map(_.requestId).getOrElse("")

  val pipelineRunner = new RequestPipelineRunner(requestId)

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    dataResolver
      .resolveByModelAndIdWithoutValidation(model, id)
      .andThen {
        case Success(x) => deletedItem = x.map(dataItem => dataItem.copy(userData = GraphcoolDataTypes.fromSql(dataItem.userData, model.fields)))
      }
      .flatMap(_ => {

        val sqlMutactions        = SqlMutactions(dataResolver).getMutactionsForDelete(model, project, id, deletedItem.getOrElse(DataItem(id)))
        val transactionMutaction = Transaction(sqlMutactions, dataResolver)

        val actionMutactions: List[ActionWebhookForDeleteDataItem] = extractActions

        // beware: ActionWebhookForDeleteDataItem requires prepareData to be awaited before being executed
        Future
          .sequence(actionMutactions.map(_.prepareData))
          .map(_ => {
            val algoliaSyncQueryMutactions = AlgoliaSyncQueries.extract(dataResolver, project, model, id, "delete")

            val nodeData: Map[String, Any] = deletedItem
              .map(_.userData)
              .getOrElse(Map.empty[String, Option[Any]])
              .collect {
                case (key, Some(value)) => (key, value)
              } + ("id" -> id)

            val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions)

            val sssActions = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId)

            val fileMutaction: List[S3DeleteFIle] = model.name match {
              case "File" => List(S3DeleteFIle(model, project, nodeData("secret").asInstanceOf[String]))
              case _      => List()
            }

            List(
              MutactionGroup(mutactions = List(transactionMutaction), async = false),
              MutactionGroup(mutactions = actionMutactions ++ sssActions ++ algoliaSyncQueryMutactions ++ fileMutaction ++ subscriptionMutactions, async = true)
            )
          })

      })
  }

  private def generatePermissionQueryArguments(existingNode: Option[DataItem]) = {
    model.scalarFields.flatMap(
      field =>
        List(
          PermissionQueryArg(s"$$old_${field.name}", existingNode.flatMap(_.getOption(field.name)).getOrElse(""), field.typeIdentifier),
          PermissionQueryArg(s"$$node_${field.name}", existingNode.flatMap(_.getOption(field.name)).getOrElse(""), field.typeIdentifier)
      ))
  }

  override def checkPermissions(authenticatedRequest: Option[AuthenticatedRequest]): Future[Boolean] = {
    def normalPermissions = ModelPermissions.checkPermissionsForDelete(model, authenticatedRequest, project)

    def customPermissions = {
      val filteredPermissions = model.permissions
        .filter(_.isActive)
        .filter(_.operation == ModelOperation.Delete)
      dataResolver
        .resolveByModelAndIdWithoutValidation(model, id)
        .flatMap(existingNode => {
          permissionValidator.checkModelQueryPermissions(project,
                                                         filteredPermissions,
                                                         authenticatedRequest,
                                                         id,
                                                         generatePermissionQueryArguments(existingNode),
                                                         alwaysQueryMasterDatabase = true)
        })
    }

    normalPermissions match {
      case true  => Future.successful(true)
      case false => customPermissions
    }
  }

  override def checkPermissionsAfterPreparingMutactions(authenticatedRequest: Option[AuthenticatedRequest], mutactions: List[Mutaction]): Future[Unit] = {
    RelationMutationPermissions.checkAllPermissions(project, mutactions, authenticatedRequest)
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    val dataItem = deletedItem.get
    for {
      transformedResult <- pipelineRunner.runTransformPayload(project = project,
                                                              model = model,
                                                              operation = RequestPipelineOperation.DELETE,
                                                              values = RequestPipelineRunner.dataItemToArgumentValues(dataItem, model))
    } yield {
      ReturnValue(RequestPipelineRunner.argumentValuesToDataItem(transformedResult, dataItem.id, model))
    }
  }

  private def extractActions: List[ActionWebhookForDeleteDataItem] = {
    project.actions
      .filter(_.isActive)
      .filter(_.triggerMutationModel.exists(_.modelId == model.id))
      .filter(_.triggerMutationModel.exists(_.mutationType == ActionTriggerMutationModelMutationType.Delete))
      .map {
        case action if action.handlerWebhook.get.isAsync =>
          ActionWebhookForDeleteDataItemAsync(
            model = model,
            project = project,
            nodeId = id,
            action = action,
            mutationId = mutationId,
            requestId = requestId
          )
        case action if !action.handlerWebhook.get.isAsync =>
          ActionWebhookForDeleteDataItemSync(
            model = model,
            project = project,
            nodeId = id,
            action = action,
            mutationId = mutationId,
            requestId = requestId
          )
      }
  }
}
