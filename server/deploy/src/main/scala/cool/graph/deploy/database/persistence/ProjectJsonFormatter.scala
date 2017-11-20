package cool.graph.deploy.database.persistence

import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import cool.graph.shared.models.{
  ActionTriggerMutationModelMutationType,
  BooleanConstraint,
  FieldConstraint,
  FieldConstraintType,
  IntegrationType,
  ModelPermission,
  NumberConstraint,
  RequestPipelineOperation,
  StringConstraint,
  TypeIdentifier,
  UserType,
  _
}
import play.api.libs.json.{Format, JsObject, JsValue, Json}

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
  implicit lazy val function    = failingFormat[Function]
  implicit lazy val integration = failingFormat[Integration]

  // MODELS
  implicit lazy val numberConstraint  = Json.format[NumberConstraint]
  implicit lazy val booleanConstraint = Json.format[BooleanConstraint]
  implicit lazy val stringConstraint  = Json.format[StringConstraint]
  implicit lazy val listConstraint    = Json.format[ListConstraint]
  implicit lazy val fieldConstraint = new Format[FieldConstraint] {
    val discriminatorField = "constraintType"

    override def reads(json: JsValue) = {
      for {
        constraintType <- (json \ discriminatorField).validate[FieldConstraintType]
      } yield {
        constraintType match {
          case FieldConstraintType.STRING  => json.as[StringConstraint]
          case FieldConstraintType.NUMBER  => json.as[NumberConstraint]
          case FieldConstraintType.BOOLEAN => json.as[BooleanConstraint]
          case FieldConstraintType.LIST    => json.as[ListConstraint]
          case unknown @ _                 => sys.error(s"Unmarshalling issue for FieldConstraintType with $unknown")
        }
      }
    }

    override def writes(o: FieldConstraint) = o match {
      case constraint: NumberConstraint  => addTypeDiscriminator(numberConstraint.writes(constraint), FieldConstraintType.NUMBER)
      case constraint: BooleanConstraint => addTypeDiscriminator(booleanConstraint.writes(constraint), FieldConstraintType.BOOLEAN)
      case constraint: StringConstraint  => addTypeDiscriminator(stringConstraint.writes(constraint), FieldConstraintType.STRING)
      case constraint: ListConstraint    => addTypeDiscriminator(listConstraint.writes(constraint), FieldConstraintType.LIST)
    }

    private def addTypeDiscriminator(jsObject: JsObject, constraintType: FieldConstraintType): JsValue = {
      jsObject + (discriminatorField -> fieldConstraintType.writes(constraintType))
    }
  }

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
