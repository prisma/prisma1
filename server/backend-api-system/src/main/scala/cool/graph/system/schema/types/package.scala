package cool.graph.system.schema

import cool.graph.shared.models
import cool.graph.shared.models.ModelParser
import cool.graph.system.SystemUserContext
import cool.graph.system.database.finder.ProjectFinder
import cool.graph.system.schema.types.ActionTriggerMutationModel.ActionTriggerMutationModelContext
import cool.graph.system.schema.types.AlgoliaSyncQuery.AlgoliaSyncQueryContext
import cool.graph.system.schema.types.Function.FunctionInterface
import cool.graph.system.schema.types.Model.ModelContext
import cool.graph.system.schema.types.ModelPermission.ModelPermissionContext
import cool.graph.system.schema.types.Relation.RelationContext
import cool.graph.system.schema.types.RelationPermission.RelationPermissionContext
import cool.graph.system.schema.types.SearchProviderAlgolia.SearchProviderAlgoliaContext
import cool.graph.system.schema.types._Action.ActionContext
import cool.graph.system.schema.types._Field.FieldContext
import sangria.relay._
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

package object types {

  val NodeDefinition(nodeInterface, nodeField, nodeRes) = Node.definitionById(
    resolve = (id: String, ctx: Context[SystemUserContext, Unit]) => {
      val clientId = ctx.ctx.getClient.id

      implicit val internalDatabase = ctx.ctx.internalDatabase
      implicit val projectResolver  = ctx.ctx.projectResolver

      ctx.ctx.getTypeName(id).flatMap {
        case Some("Client") if ctx.ctx.getClient.id == id =>
          Future.successful(Some(ctx.ctx.getClient))
        case Some("Project") => {
          val project: Future[models.Project] = ProjectFinder.loadById(clientId, id)
          project.map(Some(_))
        }
        case Some("Model") => {
          val project: Future[models.Project] = ProjectFinder.loadByModelId(clientId, id)
          project.map { project =>
            ModelParser.model(project, id, ctx.ctx.injector)
          }
        }
        case Some("Field") => {
          val project: Future[models.Project] = ProjectFinder.loadByFieldId(clientId, id)
          project.map { project =>
            ModelParser
              .field(project, id, ctx.ctx.injector)
              .map(FieldContext(project, _))
          }
        }
        case Some("Action") => {
          val project: Future[models.Project] = ProjectFinder.loadByActionId(clientId, id)
          project.map { project =>
            ModelParser
              .action(project, id)
              .map(ActionContext(project, _))
          }
        }
        case Some("Relation") => {
          val project: Future[models.Project] = ProjectFinder.loadByRelationId(clientId, id)
          project.map { project =>
            ModelParser
              .relation(project, id, ctx.ctx.injector)
              .map(rel => RelationContext(project, rel))
          }
        }
        case Some("ActionTriggerMutationModel") => {
          val project: Future[models.Project] = ProjectFinder.loadByActionTriggerMutationModelId(clientId, id)
          project.map { project =>
            ModelParser
              .actionTriggerMutationModel(project, id)
              .map(ActionTriggerMutationModelContext(project, _))
          }
        }
        case Some("ActionTriggerMutationRelation") => {
          val project: Future[models.Project] = ProjectFinder.loadByActionTriggerMutationRelationId(clientId, id)
          project.map { project =>
            ModelParser.actionTriggerMutationRelation(project, id)
          }
        }
        case Some("ActionHandlerWebhook") => {
          val project: Future[models.Project] = ProjectFinder.loadByActionHandlerWebhookId(clientId, id)
          project.map { project =>
            ModelParser.actionHandlerWebhook(project, id)
          }
        }
        case Some("Function") => {
          val project: Future[models.Project] = ProjectFinder.loadByFunctionId(clientId, id)
          project.map { project =>
            ModelParser
              .function(project, id)
              .map(Function.mapToContext(project, _))
          }
        }
        case Some("ModelPermission") => {
          val project: Future[models.Project] = ProjectFinder.loadByModelPermissionId(clientId, id)
          project.map { project =>
            ModelParser
              .modelPermission(project, id)
              .map(ModelPermissionContext(project, _))
          }
        }
        case Some("RelationPermission") => {
          val project: Future[models.Project] = ProjectFinder.loadByRelationPermissionId(clientId, id)
          project.map { project =>
            ModelParser
              .relationPermission(project, id, ctx.ctx.injector)
              .map(RelationPermissionContext(project, _))
          }
        }
        case Some("Integration") => {
          val project: Future[models.Project] = ProjectFinder.loadByIntegrationId(clientId, id)
          project.map { project =>
            ModelParser
              .integration(project, id)
              .map {
                case x: models.SearchProviderAlgolia => SearchProviderAlgoliaContext(project, x)
                case x                               => x
              }
          }
        }
        case Some("AlgoliaSyncQuery") => {
          val project: Future[models.Project] = ProjectFinder.loadByAlgoliaSyncQueryId(clientId, id)
          project.map { project =>
            {
              ModelParser
                .algoliaSyncQuery(project, id)
                .map(sync => AlgoliaSyncQueryContext(project, sync))
            }
          }
        }
        case Some("Seat") => {
          val project: Future[models.Project] = ProjectFinder.loadBySeatId(clientId, id)
          project.map { project =>
            ModelParser.seat(project, id)
          }
        }
        case Some("PackageDefinition") => {
          val project: Future[models.Project] = ProjectFinder.loadByPackageDefinitionId(clientId, id)
          project.map { project =>
            ModelParser.packageDefinition(project, id)
          }
        }
        case Some("Viewer") =>
          Future.successful(Some(ViewerModel()))
        case x =>
          println(x)
          Future.successful(None)
      }
    },
    possibleTypes = Node.possibleNodeTypes[SystemUserContext, Node](
//          ClientType,
      ProjectType,
      ModelType,
      FieldType,
      ActionType,
      ActionTriggerMutationModelType,
      ActionTriggerMutationRelationType,
      ActionHandlerWebhookType,
      RelationType,
      AuthProviderType,
      ModelPermissionType,
      RelationPermissionType,
      SearchProviderAlgoliaType,
      AlgoliaSyncQueryType,
      RequestPipelineMutationFunctionType,
      ServerSideSubscriptionFunctionType,
      SchemaExtensionFunctionType,
      StringConstraintType,
      BooleanConstraintType,
      NumberConstraintType,
      ListConstraintType
    )
  )

  lazy val CustomerSourceType = CustomerSource.Type
  lazy val UserTypeType       = UserType.Type
//  lazy val ClientType = Customer.Type
  lazy val rootTokenType                       = rootToken.Type
  lazy val ProjectType                         = Project.Type
  lazy val ProjectDatabaseType                 = ProjectDatabase.Type
  lazy val RegionType                          = Region.Type
  lazy val ModelType                           = Model.Type
  lazy val OurEnumType                         = Enum.Type
  lazy val FieldType                           = _Field.Type
  lazy val ModelPermissionType                 = ModelPermission.Type
  lazy val RelationPermissionType              = RelationPermission.Type
  lazy val RelationType                        = Relation.Type
  lazy val FunctionInterfaceType               = Function.Type
  lazy val RequestPipelineMutationFunctionType = RequestPipelineMutationFunction.Type
  lazy val ServerSideSubscriptionFunctionType  = ServerSideSubscriptionFunction.Type
  lazy val SchemaExtensionFunctionType         = SchemaExtensionFunction.Type
  lazy val LogType                             = Log.Type
  lazy val LogStatusType                       = LogStatus.Type
  lazy val RelationFieldMirrorType             = RelationFieldMirror.Type
  lazy val AuthProviderType                    = AuthProvider.Type
  lazy val ActionType                          = _Action.Type
  lazy val TriggerTypeType                     = TriggerType.Type
  lazy val HandlerTypeType                     = HandlerType.Type
  lazy val ActionTriggerMutationModelType      = ActionTriggerMutationModel.Type
  lazy val ModelMutationTypeType               = ModelMutationType.Type
  lazy val RelationMutationTypeType            = RelationMutationType.Type
  lazy val ActionTriggerMutationRelationType   = ActionTriggerMutationRelation.Type
  lazy val ActionHandlerWebhookType            = ActionHandlerWebhook.Type
  lazy val SearchProviderAlgoliaType           = SearchProviderAlgolia.Type
  lazy val AlgoliaSyncQueryType                = AlgoliaSyncQuery.Type
  lazy val IntegrationInterfaceType            = Integration.Type
  lazy val SeatStatusType                      = SeatStatus.Type
  lazy val SeatType                            = Seat.Type
  lazy val PackageDefinitionType               = PackageDefinition.Type
  lazy val FeatureToggleType                   = FeatureToggle.Type
  lazy val FieldConstraintType                 = FieldConstraint.Type
  lazy val StringConstraintType                = StringConstraint.Type
  lazy val NumberConstraintType                = NumberConstraint.Type
  lazy val BooleanConstraintType               = BooleanConstraint.Type
  lazy val ListConstraintType                  = ListConstraint.Type
  lazy val HistogramPeriodType                 = HistogramPeriod.Type

  //  lazy val ViewerType = Viewer.Type

//  lazy val ConnectionDefinition(clientEdge, clientConnection) = Connection
//    .definition[UserContext, Connection, models.Client]("Client", ClientType)

  lazy val ConnectionDefinition(projectEdge, projectConnection) =
    Connection.definition[SystemUserContext, Connection, models.Project]("Project", ProjectType)

  lazy val ConnectionDefinition(modelEdge, modelConnection) = Connection
    .definition[SystemUserContext, Connection, ModelContext]("Model", ModelType)

  lazy val ConnectionDefinition(enumEdge, enumConnection) = Connection
    .definition[SystemUserContext, Connection, models.Enum]("Enum", OurEnumType)

  lazy val ConnectionDefinition(packageDefinitionEdge, packageDefinitionConnection) = Connection
    .definition[SystemUserContext, Connection, models.PackageDefinition]("PackageDefinition", PackageDefinitionType)

  lazy val ConnectionDefinition(algoliaSyncQueryEdge, algoliaSyncQueryConnection) = Connection
    .definition[SystemUserContext, Connection, AlgoliaSyncQueryContext]("AlgoliaSyncQuery", AlgoliaSyncQueryType)

  lazy val ConnectionDefinition(projectFieldEdge, projectFieldConnection) =
    Connection
      .definition[SystemUserContext, Connection, FieldContext]("Field", FieldType)

  lazy val ConnectionDefinition(relationEdge, relationConnection) =
    Connection.definition[SystemUserContext, Connection, RelationContext]("Relation", RelationType)

  lazy val ConnectionDefinition(functionEdge, functionConnection) =
    Connection.definition[SystemUserContext, Connection, FunctionInterface]("Function", FunctionInterfaceType)

  lazy val ConnectionDefinition(logEdge, logConnection) =
    Connection.definition[SystemUserContext, Connection, models.Log]("Log", LogType)

  lazy val ConnectionDefinition(relationFieldMirrorEdge, relationFieldMirrorConnection) =
    Connection
      .definition[SystemUserContext, Connection, models.RelationFieldMirror]("RelationFieldMirror", RelationFieldMirrorType)

  lazy val ConnectionDefinition(actionEdge, actionConnection) = Connection
    .definition[SystemUserContext, Connection, ActionContext]("Action", ActionType)

  lazy val ConnectionDefinition(authProviderEdge, authProviderConnection) =
    Connection
      .definition[SystemUserContext, Connection, models.AuthProvider]("AuthProvider", AuthProviderType)

  lazy val ConnectionDefinition(fieldEdge, fieldConnection) = Connection
    .definition[SystemUserContext, Connection, FieldContext]("Field", FieldType)

  lazy val ConnectionDefinition(modelPermissionEdge, modelPermissionConnection) =
    Connection
      .definition[SystemUserContext, Connection, ModelPermissionContext]("ModelPermission", ModelPermissionType)

  lazy val ConnectionDefinition(relationPermissionEdge, relationPermissionConnection) =
    Connection
      .definition[SystemUserContext, Connection, RelationPermissionContext]("RelationPermission", RelationPermissionType)

  lazy val ConnectionDefinition(rootTokenEdge, rootTokenConnection) =
    Connection
      .definition[SystemUserContext, Connection, models.RootToken]("PermanentAuthToken", rootTokenType)

  lazy val ConnectionDefinition(integrationEdge, integrationConnection) =
    Connection
      .definition[SystemUserContext, Connection, models.Integration]("Integration", IntegrationInterfaceType)

  lazy val ConnectionDefinition(seatEdge, seatConnection) =
    Connection.definition[SystemUserContext, Connection, models.Seat]("Seat", SeatType)

  lazy val ConnectionDefinition(featureToggleEdge, featureToggleConnection) =
    Connection.definition[SystemUserContext, Connection, models.FeatureToggle]("FeatureToggle", FeatureToggleType)

  def idField[Ctx, T: Identifiable]: Field[Ctx, T] =
    Field("id", IDType, resolve = ctx => implicitly[Identifiable[T]].id(ctx.value))
}
