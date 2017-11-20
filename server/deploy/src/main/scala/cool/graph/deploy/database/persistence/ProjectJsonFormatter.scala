package cool.graph.deploy.database.persistence

import cool.graph.shared.models.{
  ActionTriggerMutationModelMutationType,
  FieldConstraint,
  FieldConstraintType,
  IntegrationType,
  ModelPermission,
  RequestPipelineOperation,
  TypeIdentifier,
  UserType,
  _
}
import play.api.libs.json.{Format, JsValue, Json}

object ProjectJsonFormatter {
  import cool.graph.util.json.JsonUtils.{enumFormat, DateTimeFormat}

  // ENUMS
  implicit lazy val seatStatus                                = enumFormat(SeatStatus)
  implicit lazy val regionFormat                              = enumFormat(Region)
  implicit lazy val logStatus                                 = enumFormat(LogStatus)
  implicit lazy val requestPipelineOperation                  = enumFormat(RequestPipelineOperation)
  implicit lazy val integrationType                           = enumFormat(IntegrationType)
  implicit lazy val integrationName                           = enumFormat(IntegrationName)
  implicit lazy val relationSide                              = enumFormat(RelationSide)
  implicit lazy val typeIdentifier                            = enumFormat(TypeIdentifier)
  implicit lazy val fieldConstraintType                       = enumFormat(FieldConstraintType)
  implicit lazy val userType                                  = enumFormat(UserType)
  implicit lazy val modelMutationType                         = enumFormat(ModelMutationType)
  implicit lazy val customRule                                = enumFormat(CustomRule)
  implicit lazy val modelOperation                            = enumFormat(ModelOperation)
  implicit lazy val actionHandlerType                         = enumFormat(ActionHandlerType)
  implicit lazy val actionTriggerType                         = enumFormat(ActionTriggerType)
  implicit lazy val actionTriggerMutationModelMutationType    = enumFormat(ActionTriggerMutationModelMutationType)
  implicit lazy val actionTriggerMutationRelationMutationType = enumFormat(ActionTriggerMutationRelationMutationType)

  // FAILING STUBS
  implicit lazy val fieldConstraint = failingFormat[FieldConstraint]
  implicit lazy val function        = failingFormat[Function]
  implicit lazy val integration     = failingFormat[Integration]

  // MODELS
  implicit lazy val projectDatabase               = Json.format[ProjectDatabase]
  implicit lazy val modelPermission               = Json.format[ModelPermission]
  implicit lazy val relationFieldMirror           = Json.format[RelationFieldMirror]
  implicit lazy val relationPermission            = Json.format[RelationPermission]
  implicit lazy val relation                      = Json.format[Relation]
  implicit lazy val enum                          = Json.format[Enum]
  implicit lazy val field                         = Json.format[Field]
  implicit lazy val model                         = Json.format[Model]
  implicit lazy val actionHandlerWebhook          = Json.format[ActionHandlerWebhook]
  implicit lazy val actionTriggerMutationModel    = Json.format[ActionTriggerMutationModel]
  implicit lazy val actionTriggerMutationRelation = Json.format[ActionTriggerMutationRelation]
  implicit lazy val action                        = Json.format[Action]
  implicit lazy val rootToken                     = Json.format[RootToken]
  implicit lazy val seat                          = Json.format[Seat]
  implicit lazy val packageDefinition             = Json.format[PackageDefinition]
  implicit lazy val featureToggle                 = Json.format[FeatureToggle]
  implicit lazy val projectFormat                 = Json.format[Project]

  def failingFormat[T] = new Format[T] {

    override def reads(json: JsValue) = fail
    override def writes(o: T)         = fail

    def fail = sys.error("This JSON Formatter always fails.")
  }
}
