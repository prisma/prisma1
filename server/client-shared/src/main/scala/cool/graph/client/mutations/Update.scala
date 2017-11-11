package cool.graph.client.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.adapters.GraphcoolDataTypes
import cool.graph.client.authorization.{ModelPermissions, PermissionValidator, RelationMutationPermissions}
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.mutations.definitions.UpdateDefinition
import cool.graph.client.requestPipeline.RequestPipelineRunner
import cool.graph.client.schema.InputTypesBuilder
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Action => ActionModel, _}
import sangria.schema
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Update(model: Model, project: Project, args: schema.Args, dataResolver: DataResolver, argumentSchema: ArgumentSchema)(implicit inj: Injector)
    extends ClientMutation(model, args, dataResolver, argumentSchema)
    with Injectable {

  override val mutationDefinition = UpdateDefinition(argumentSchema, project, InputTypesBuilder(project, argumentSchema))

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")
  val permissionValidator                      = new PermissionValidator(project)

  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("input") match { // TODO: input token is probably relay specific?
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }
    CoolArgs(argsPointer, argumentSchema, model, project)
  }

  val id: Id            = coolArgs.getFieldValueAs[Id]("id").get.get
  val requestId: String = dataResolver.requestContext.map(_.requestId).getOrElse("")

  val pipelineRunner = new RequestPipelineRunner(requestId)

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    dataResolver.resolveByModelAndIdWithoutValidation(model, id) map {
      case Some(dataItem) =>
        val validatedDataItem = dataItem.copy(userData = GraphcoolDataTypes.fromSql(dataItem.userData, model.fields))

        val sqlMutactions: List[ClientSqlMutaction] =
          SqlMutactions(dataResolver).getMutactionsForUpdate(project, model, coolArgs, id, validatedDataItem, requestId)

        val transactionMutaction = Transaction(sqlMutactions, dataResolver)

        val updateMutactionOpt: Option[UpdateDataItem] = sqlMutactions.collect { case x: UpdateDataItem => x }.headOption

        val updateMutactions = sqlMutactions.collect { case x: UpdateDataItem => x }

        val actionMutactions = ActionWebhooks.extractFromUpdateMutactions(project = project,
                                                                          mutactions = updateMutactions,
                                                                          mutationId = mutationId,
                                                                          requestId = requestId,
                                                                          previousValues = validatedDataItem)

        val fileMutaction: Option[S3UpdateFileName] = for {
          updateMutaction <- updateMutactionOpt
          if model.name == "File" && updateMutaction.namesOfUpdatedFields.contains("name")
        } yield {
          val newFileName = updateMutaction.values.find(_.name == "name").get.value.asInstanceOf[Option[String]].get
          S3UpdateFileName(model, project, id, newFileName, dataResolver)
        }

        val algoliaSyncQueryMutactions = AlgoliaSyncQueries.extract(dataResolver, project, model, id, "update")

        val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions)

        val sssActions = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId)

        List(
          MutactionGroup(mutactions = List(transactionMutaction), async = false),
          MutactionGroup(mutactions = actionMutactions.toList ++ sssActions ++ fileMutaction ++ algoliaSyncQueryMutactions ++ subscriptionMutactions,
                         async = true)
        )

      case None =>
        List(
          MutactionGroup(
            mutactions = List(
              UpdateDataItem(project = project,
                             model = model,
                             id = id,
                             values = List.empty,
                             originalArgs = None,
                             previousValues = DataItem(id),
                             itemExists = false)),
            async = false
          ),
          MutactionGroup(mutactions = List.empty, async = true)
        )
    }
  }

  override def checkPermissions(authenticatedRequest: Option[AuthenticatedRequest]): Future[Boolean] = {
    def checkCustomPermissionsForField(field: Field): Future[Boolean] = {
      dataResolver.resolveByModelAndIdWithoutValidation(model, id).flatMap { existingNode =>
        val filteredPermissions = model.permissions
          .filter(_.isActive)
          .filter(_.operation == ModelOperation.Update)
          .filter(p => p.applyToWholeModel || p.fieldIds.contains(field.id))

        permissionValidator.checkModelQueryPermissions(
          project,
          filteredPermissions,
          authenticatedRequest,
          id,
          coolArgs.permissionQueryArgsForNewAndOldFieldValues(mutationDefinition, existingNode),
          alwaysQueryMasterDatabase = true
        )
      }
    }

    val normalPermissions = ModelPermissions.checkPermissionsForUpdate(model, coolArgs, authenticatedRequest, project)

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

  override def getReturnValue: Future[ReturnValue] = {

    def ensureReturnValue(returnValue: ReturnValueResult): ReturnValue = {
      returnValue match {
        case x: NoReturnValue => throw UserAPIErrors.DataItemDoesNotExist(model.name, id)
        case x: ReturnValue   => x
      }
    }

    for {
      returnValueResult <- returnValueById(model, id)
      dataItem          = ensureReturnValue(returnValueResult).dataItem
      transformedResult <- pipelineRunner.runTransformPayload(
                            project = project,
                            model = model,
                            operation = RequestPipelineOperation.UPDATE,
                            values = RequestPipelineRunner.dataItemToArgumentValues(dataItem, model)
                          )
    } yield {
      ReturnValue(RequestPipelineRunner.argumentValuesToDataItem(transformedResult, dataItem.id, model))
    }
  }
}
