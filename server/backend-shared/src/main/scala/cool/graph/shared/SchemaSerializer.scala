package cool.graph.shared

import cool.graph.GCDataTypes.{GCJsonConverter, GCStringConverter, GCValue}
import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import cool.graph.shared.models.IntegrationName.IntegrationName
import cool.graph.shared.models.RelationSide.RelationSide
import cool.graph.shared.models.TypeIdentifier.TypeIdentifier
import cool.graph.shared.models._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat, _}

import scala.util.Try

object SchemaSerializer {
  type ClientAndProjectIds = (Client, List[String])

  class EnumJsonConverter[T <: scala.Enumeration](enu: T) extends RootJsonFormat[T#Value] {
    override def write(obj: T#Value): JsValue = JsString(obj.toString)

    override def read(json: JsValue): T#Value = {
      json match {
        case JsString(txt) => enu.withName(txt)
        case somethingElse =>
          throw DeserializationException(s"Expected a value from enum $enu instead of $somethingElse")
      }
    }
  }

  object EnumFormats {
    implicit val CustomerSourceConverter                            = new EnumJsonConverter(CustomerSource)
    implicit val RegionConverter                                    = new EnumJsonConverter(Region)
    implicit val TypeIdentifierConverter                            = new EnumJsonConverter(TypeIdentifier)
    implicit val RelationSideConverter                              = new EnumJsonConverter(RelationSide)
    implicit val UserTypeConverter                                  = new EnumJsonConverter(UserType)
    implicit val CustomRuleConverter                                = new EnumJsonConverter(CustomRule)
    implicit val ModelOperationConverter                            = new EnumJsonConverter(ModelOperation)
    implicit val ActionTriggerTypeConverter                         = new EnumJsonConverter(ActionTriggerType)
    implicit val ActionHandlerTypeConverter                         = new EnumJsonConverter(ActionHandlerType)
    implicit val ActionTriggerMutationModelMutationTypeConverter    = new EnumJsonConverter(ActionTriggerMutationModelMutationType)
    implicit val ActionTriggerMutationRelationMutationTypeConverter = new EnumJsonConverter(ActionTriggerMutationRelationMutationType)
    implicit val IntegrationNameConverter                           = new EnumJsonConverter(IntegrationName)
    implicit val IntegrationTypeConverter                           = new EnumJsonConverter(IntegrationType)
    implicit val SeatStatusConverter                                = new EnumJsonConverter(SeatStatus)
    implicit val FieldConstraintTypeConverter                       = new EnumJsonConverter(FieldConstraintType)
    implicit val FunctionBindingConverter                           = new EnumJsonConverter(FunctionBinding)
    implicit val FunctionTypeConverter                              = new EnumJsonConverter(FunctionType)
    implicit val RequestPipelineOperationConverter                  = new EnumJsonConverter(RequestPipelineOperation)
  }

  object CaseClassFormats extends DefaultJsonProtocol {
    import EnumFormats._

    implicit object DateTimeFormat extends RootJsonFormat[DateTime] {

      val formatter = ISODateTimeFormat.basicDateTime

      def write(obj: DateTime): JsValue = {
        JsString(formatter.print(obj))
      }

      def read(json: JsValue): DateTime = json match {
        case JsString(s) =>
          try {
            formatter.parseDateTime(s)
          } catch {
            case t: Throwable => error(s)
          }
        case _ =>
          error(json.toString())
      }

      def error(v: Any): DateTime = {
        val example = formatter.print(0)
        deserializationError(f"'$v' is not a valid date value. Dates must be in compact ISO-8601 format, e.g. '$example'")
      }
    }

    implicit lazy val projectDatabaseFormat               = jsonFormat4(ProjectDatabase.apply)
    implicit val enumFormat                               = jsonFormat3(Enum.apply)
    implicit val relationFieldMirrorFormat                = jsonFormat3(RelationFieldMirror.apply)
    implicit val relationPermissionFormat                 = jsonFormat11(RelationPermission.apply)
    implicit lazy val actionHandlerWebhookFormat          = jsonFormat3(ActionHandlerWebhook.apply)
    implicit lazy val actionTriggerMutationModelFormat    = jsonFormat4(ActionTriggerMutationModel.apply)
    implicit lazy val actionTriggerMutationRelationFormat = jsonFormat4(ActionTriggerMutationRelation.apply)
    implicit lazy val actionFormat                        = jsonFormat8(Action.apply)
    implicit lazy val permanentAuthTokenFormat            = jsonFormat4(RootToken.apply)
    implicit lazy val seatFormat                          = jsonFormat6(Seat.apply)
    implicit lazy val packageDefinitionFormat             = jsonFormat4(PackageDefinition.apply)

    implicit object FieldConstraintFormat extends RootJsonFormat[FieldConstraint] {

      implicit val stringConstraintFormat =
        jsonFormat(StringConstraint.apply,
                   "id",
                   "fieldId",
                   "equalsString",
                   "oneOfString",
                   "minLength",
                   "maxLength",
                   "startsWith",
                   "endsWith",
                   "includes",
                   "regex")

      implicit val numberConstraintFormat =
        jsonFormat(NumberConstraint.apply, "id", "fieldId", "equalsNumber", "oneOfNumber", "min", "max", "exclusiveMin", "exclusiveMax", "multipleOf")

      implicit val booleanConstraintFormat = jsonFormat(BooleanConstraint.apply, "id", "fieldId", "equalsBoolean")

      implicit val listConstraintFormat =
        jsonFormat(ListConstraint.apply, "id", "fieldId", "uniqueItems", "minItems", "maxItems")

      private def addTypeDiscriminator(value: JsValue, constraintType: FieldConstraintType): JsValue = {
        JsObject(value.asJsObject.fields + ("constraintType" -> constraintType.toJson))
      }

      def write(obj: FieldConstraint) = obj match {
        case x: StringConstraint  => addTypeDiscriminator(x.toJson, FieldConstraintType.STRING)
        case x: NumberConstraint  => addTypeDiscriminator(x.toJson, FieldConstraintType.NUMBER)
        case x: BooleanConstraint => addTypeDiscriminator(x.toJson, FieldConstraintType.BOOLEAN)
        case x: ListConstraint    => addTypeDiscriminator(x.toJson, FieldConstraintType.LIST)
        case unknown @ _          => serializationError(s"Marshalling issue with $unknown")
      }

      def read(value: JsValue): FieldConstraint = {
        val typeDiscriminator = value.asJsObject().fields("constraintType").convertTo[FieldConstraintType]
        typeDiscriminator match {
          case FieldConstraintType.STRING  => value.asJsObject.convertTo[StringConstraint]
          case FieldConstraintType.NUMBER  => value.asJsObject.convertTo[NumberConstraint]
          case FieldConstraintType.BOOLEAN => value.asJsObject.convertTo[BooleanConstraint]
          case FieldConstraintType.LIST    => value.asJsObject.convertTo[ListConstraint]
          case unknown @ _                 => deserializationError(s"Unmarshalling issue with $unknown ")
        }
      }
    }

    implicit val relationFormat = jsonFormat7(Relation.apply)

    implicit object FieldFormat extends RootJsonFormat[Field] {

      def write(obj: Field) = {

        val convertedDefaultValue = obj.defaultValue.map(GCJsonConverter(obj.typeIdentifier, obj.isList).fromGCValue).getOrElse(JsNull)
        JsObject(
          "id"             -> JsString(obj.id),
          "name"           -> JsString(obj.name),
          "typeIdentifier" -> obj.typeIdentifier.toJson,
          "description"    -> obj.description.toJson,
          "isRequired"     -> JsBoolean(obj.isRequired),
          "isList"         -> JsBoolean(obj.isList),
          "isUnique"       -> JsBoolean(obj.isUnique),
          "isSystem"       -> JsBoolean(obj.isSystem),
          "isReadonly"     -> JsBoolean(obj.isReadonly),
          "enum"           -> obj.enum.toJson,
          "defaultValue"   -> convertedDefaultValue,
          "relation"       -> obj.relation.toJson,
          "relationSide"   -> obj.relationSide.toJson,
          "constraints"    -> obj.constraints.toJson
        )
      }

      def read(value: JsValue): Field = {
        val f              = value.asJsObject.fields
        val typeIdentifier = f("typeIdentifier").convertTo[TypeIdentifier]
        val isList         = f("isList").convertTo[Boolean]

        val defaultValue: Option[GCValue] = f("defaultValue") match {
          case JsNull      => None
          case x: JsString => Some(GCStringConverter(typeIdentifier, isList).toGCValue(x.value).get)
          case x: JsValue  => Some(GCJsonConverter(typeIdentifier, isList).toGCValue(x).get)
        }

        Field(
          id = f("id").convertTo[String],
          name = f("name").convertTo[String],
          typeIdentifier = typeIdentifier,
          description = f("description").convertTo[Option[String]],
          isRequired = f("isRequired").convertTo[Boolean],
          isList = isList,
          isUnique = f("isUnique").convertTo[Boolean],
          isSystem = f("isSystem").convertTo[Boolean],
          isReadonly = f("isReadonly").convertTo[Boolean],
          enum = f("enum").convertTo[Option[Enum]],
          defaultValue = defaultValue,
          relation = f("relation").convertTo[Option[Relation]],
          relationSide = f("relationSide").convertTo[Option[RelationSide]],
          constraints = f("constraints").convertTo[List[FieldConstraint]]
        )
      }
    }

    implicit val modelPermissionFormat = jsonFormat12(ModelPermission.apply)

    implicit object ModelFormat extends RootJsonFormat[Model] {

      def write(obj: Model) = {
        JsObject(
          "id"             -> JsString(obj.id),
          "name"           -> JsString(obj.name),
          "description"    -> obj.description.toJson,
          "isSystem"       -> JsBoolean(obj.isSystem),
          "fields"         -> obj.fields.toJson,
          "permissions"    -> obj.permissions.toJson,
          "fieldPositions" -> obj.fieldPositions.toJson
        )
      }

      def read(value: JsValue): Model = {
        val f = value.asJsObject.fields

        Model(
          id = f("id").convertTo[String],
          name = f("name").convertTo[String],
          description = f("description").convertTo[Option[String]],
          isSystem = f("isSystem").convertTo[Boolean],
          fields = f("fields").convertTo[List[Field]],
          permissions = f("permissions").convertTo[List[ModelPermission]],
          fieldPositions = f("fieldPositions").convertTo[List[String]]
        )
      }
    }

    implicit object AuthProviderMetaInformationFormat extends RootJsonFormat[AuthProviderMetaInformation] {
      implicit val authProviderAuth0Format  = jsonFormat4(AuthProviderAuth0)
      implicit val authProviderDigitsFormat = jsonFormat3(AuthProviderDigits)

      def write(obj: AuthProviderMetaInformation) = obj match {
        case x: AuthProviderDigits => x.toJson
        case y: AuthProviderAuth0  => y.toJson
      }

      def read(value: JsValue): AuthProviderMetaInformation = {
        value.asJsObject.fields.keys.exists(_ == "domain") match {
          case true  => value.asJsObject.convertTo[AuthProviderAuth0]
          case false => value.asJsObject.convertTo[AuthProviderDigits]
        }
      }
    }

    implicit val algoliaSyncQueryFormat = jsonFormat5(AlgoliaSyncQuery)

    implicit object AuthProviderFormat extends RootJsonFormat[AuthProvider] {

      def write(obj: AuthProvider) = {
        JsObject(
          "id"              -> JsString(obj.id),
          "subTableId"      -> JsString(obj.subTableId),
          "isEnabled"       -> JsBoolean(obj.isEnabled),
          "name"            -> obj.name.toJson,
          "metaInformation" -> obj.metaInformation.toJson
        )
      }

      def read(value: JsValue): AuthProvider = {
        val f = value.asJsObject.fields

        AuthProvider(
          id = f("id").convertTo[String],
          subTableId = f("subTableId").convertTo[String],
          isEnabled = f("isEnabled").convertTo[Boolean],
          name = f("name").convertTo[IntegrationName],
          metaInformation = f("metaInformation").convertTo[Option[AuthProviderMetaInformation]]
        )
      }
    }

    implicit object SearchProviderAlgoliaFormat extends RootJsonFormat[SearchProviderAlgolia] {

      def write(obj: SearchProviderAlgolia) = {
        JsObject(
          "id"                 -> JsString(obj.id),
          "subTableId"         -> JsString(obj.subTableId),
          "applicationId"      -> JsString(obj.applicationId),
          "apiKey"             -> JsString(obj.apiKey),
          "algoliaSyncQueries" -> obj.algoliaSyncQueries.toJson,
          "isEnabled"          -> JsBoolean(obj.isEnabled),
          "name"               -> obj.name.toJson
        )
      }

      def read(value: JsValue): SearchProviderAlgolia = {
        val f = value.asJsObject.fields

        SearchProviderAlgolia(
          id = f("id").convertTo[String],
          subTableId = f("subTableId").convertTo[String],
          applicationId = f("applicationId").convertTo[String],
          apiKey = f("apiKey").convertTo[String],
          algoliaSyncQueries = f("algoliaSyncQueries").convertTo[List[AlgoliaSyncQuery]],
          isEnabled = f("isEnabled").convertTo[Boolean],
          name = f("name").convertTo[IntegrationName]
        )
      }
    }

    implicit object IntegrationFormat extends RootJsonFormat[Integration] {

      def write(obj: Integration) = obj match {
        case x: AuthProvider          => x.toJson
        case y: SearchProviderAlgolia => y.toJson
        case unknown @ _              => serializationError(s"Marshalling issue with $unknown")
      }

      def read(value: JsValue): Integration = {
        value.asJsObject.fields.keys.exists(_ == "algoliaSyncQueries") match {
          case true  => value.asJsObject.convertTo[SearchProviderAlgolia]
          case false => value.asJsObject.convertTo[AuthProvider]
        }
      }
    }

    implicit object Auth0FunctionFormat extends RootJsonFormat[Auth0Function] {
      def write(obj: Auth0Function) = {
        JsObject(
          "code"         -> JsString(obj.code),
          "codeFilePath" -> obj.codeFilePath.toJson,
          "auth0Id"      -> JsString(obj.auth0Id),
          "url"          -> JsString(obj.url),
          "headers"      -> obj.headers.toJson
        )
      }

      def read(value: JsValue): Auth0Function = {
        val f = value.asJsObject.fields

        Auth0Function(
          code = f("code").convertTo[String],
          codeFilePath = f("codeFilePath").convertTo[Option[String]],
          auth0Id = f("auth0Id").convertTo[String],
          url = f("url").convertTo[String],
          headers = f("headers").convertTo[Seq[(String, String)]]
        )
      }
    }

    implicit object WebhookFunctionFormat extends RootJsonFormat[WebhookFunction] {
      def write(obj: WebhookFunction) = {
        JsObject(
          "url"     -> JsString(obj.url),
          "headers" -> obj.headers.toJson
        )
      }

      def read(value: JsValue): WebhookFunction = {
        val f = value.asJsObject.fields

        WebhookFunction(
          url = f("url").convertTo[String],
          headers = f("headers").convertTo[Seq[(String, String)]]
        )
      }
    }

    implicit object managedFunctionFormat extends RootJsonFormat[ManagedFunction] {
      def write(obj: ManagedFunction) = {
        obj.codeFilePath match {
          case Some(codeFilePath) =>
            JsObject(
              "codeFilePath" -> JsString(codeFilePath)
            )
          case None => JsObject.empty
        }
      }

      def read(value: JsValue): ManagedFunction = {
        val f = value.asJsObject.fields

        ManagedFunction(
          codeFilePath = f.get("codeFilePath").map(_.convertTo[String])
        )
      }
    }

    implicit object FunctionDeliveryFormat extends RootJsonFormat[FunctionDelivery] {

      def write(obj: FunctionDelivery) = obj match {
        case x: Auth0Function   => x.toJson
        case y: WebhookFunction => y.toJson
        case z: ManagedFunction =>
          z.codeFilePath match {
            case Some(codeFilePath) => JsObject("_isCodeFunction" -> JsBoolean(true), "codeFilePath" -> JsString(codeFilePath))
            case None               => JsObject("_isCodeFunction" -> JsBoolean(true))
          }
        case unknown @ _ => serializationError(s"Marshalling issue with unknown function delivery: $unknown")
      }

      def read(value: JsValue): FunctionDelivery = {
        () match {
          case _ if value.asJsObject.fields.keys.exists(_ == "auth0Id")         => value.asJsObject.convertTo[Auth0Function]
          case _ if value.asJsObject.fields.keys.exists(_ == "_isCodeFunction") => value.asJsObject.convertTo[ManagedFunction]
          case _                                                                => value.asJsObject.convertTo[WebhookFunction]
        }
      }
    }

    implicit object FunctionFormat extends RootJsonFormat[Function] {
      implicit val serversideSubscriptionFunctionFormat = jsonFormat6(ServerSideSubscriptionFunction)
      implicit val requestPipelineFunctionFormat        = jsonFormat7(RequestPipelineFunction)
      implicit val freeTypeFormat                       = jsonFormat4(FreeType)
      implicit val customMutationFunctionFormat         = jsonFormat9(CustomMutationFunction.apply)
      implicit val customQueryFunctionFormat            = jsonFormat9(CustomQueryFunction.apply)

      def write(obj: Function) = obj match {
        case obj: ServerSideSubscriptionFunction =>
          JsObject(
            "id"            -> obj.id.toJson,
            "name"          -> obj.name.toJson,
            "isActive"      -> obj.isActive.toJson,
            "query"         -> obj.query.toJson,
            "queryFilePath" -> obj.queryFilePath.toJson,
            "delivery"      -> obj.delivery.toJson,
            "binding"       -> obj.binding.toJson
          )

        case obj: RequestPipelineFunction =>
          JsObject(
            "id"        -> obj.id.toJson,
            "name"      -> obj.name.toJson,
            "isActive"  -> obj.isActive.toJson,
            "modelId"   -> obj.modelId.toJson,
            "delivery"  -> obj.delivery.toJson,
            "binding"   -> obj.binding.toJson,
            "operation" -> obj.operation.toJson
          )

        case obj: CustomMutationFunction =>
          JsObject(
            "id"             -> obj.id.toJson,
            "name"           -> obj.name.toJson,
            "isActive"       -> obj.isActive.toJson,
            "schema"         -> obj.schema.toJson,
            "schemaFilePath" -> obj.schemaFilePath.toJson,
            "delivery"       -> obj.delivery.toJson,
            "binding"        -> obj.binding.toJson,
            "mutationName"   -> obj.mutationName.toJson,
            "arguments"      -> obj.arguments.toJson,
            "payloadType"    -> obj.payloadType.toJson
          )

        case obj: CustomQueryFunction =>
          JsObject(
            "id"             -> obj.id.toJson,
            "name"           -> obj.name.toJson,
            "isActive"       -> obj.isActive.toJson,
            "schema"         -> obj.schema.toJson,
            "schemaFilePath" -> obj.schemaFilePath.toJson,
            "delivery"       -> obj.delivery.toJson,
            "binding"        -> obj.binding.toJson,
            "queryName"      -> obj.queryName.toJson,
            "arguments"      -> obj.arguments.toJson,
            "payloadType"    -> obj.payloadType.toJson
          )

        case unknown @ _ => serializationError(s"Marshalling issue with unknown function: $unknown")
      }

      def read(value: JsValue): Function = {
        val binding = value.asJsObject.fields.getOrElse("binding", sys.error(s"binding not present on function: ${value.prettyPrint}"))

        FunctionBinding.withName(binding.convertTo[String]) match {
          case FunctionBinding.CUSTOM_QUERY =>
            value.asJsObject.convertTo[CustomQueryFunction]

          case FunctionBinding.CUSTOM_MUTATION =>
            value.asJsObject.convertTo[CustomMutationFunction]

          case FunctionBinding.TRANSFORM_REQUEST | FunctionBinding.PRE_WRITE | FunctionBinding.TRANSFORM_ARGUMENT | FunctionBinding.TRANSFORM_PAYLOAD =>
            value.asJsObject.convertTo[RequestPipelineFunction]

          case FunctionBinding.SERVERSIDE_SUBSCRIPTION =>
            value.asJsObject.convertTo[ServerSideSubscriptionFunction]
        }
      }
    }

    implicit val featureToggleFormat = jsonFormat3(FeatureToggle)

    implicit object projectFormat extends RootJsonFormat[Project] {

      def write(obj: Project) = {
        JsObject(
          "id"                      -> JsString(obj.id),
          "name"                    -> JsString(obj.name),
          "projectDatabase"         -> obj.projectDatabase.toJson,
          "ownerId"                 -> obj.ownerId.toJson,
          "alias"                   -> obj.alias.toJson,
          "revision"                -> obj.revision.toJson,
          "webhookUrl"              -> obj.webhookUrl.toJson,
          "models"                  -> obj.models.toJson,
          "relations"               -> obj.relations.toJson,
          "enums"                   -> obj.enums.toJson,
          "actions"                 -> obj.actions.toJson,
          "permanentAuthTokens"     -> obj.rootTokens.toJson,
          "integrations"            -> obj.integrations.toJson,
          "seats"                   -> obj.seats.toJson,
          "allowQueries"            -> obj.allowQueries.toJson,
          "allowMutations"          -> obj.allowMutations.toJson,
          "packageDefinitions"      -> obj.packageDefinitions.toJson,
          "functions"               -> obj.functions.toJson,
          "featureToggles"          -> obj.featureToggles.toJson,
          "typePositions"           -> obj.typePositions.toJson,
          "isEjected"               -> JsBoolean(obj.isEjected),
          "hasGlobalStarPermission" -> JsBoolean(obj.hasGlobalStarPermission)
        )
      }

      def read(value: JsValue): Project = {
        val f = value.asJsObject.fields

        try {
          Project(
            id = f("id").convertTo[String],
            name = f("name").convertTo[String],
            projectDatabase = f("projectDatabase").convertTo[ProjectDatabase],
            ownerId = f("ownerId").convertTo[String],
            alias = f("alias").convertTo[Option[String]],
            revision = f("revision").convertTo[Int],
            webhookUrl = f("webhookUrl").convertTo[Option[String]],
            models = f("models").convertTo[List[Model]],
            relations = f("relations").convertTo[List[Relation]],
            enums = f("enums").convertTo[List[Enum]],
            actions = f("actions").convertTo[List[Action]],
            rootTokens = f("permanentAuthTokens").convertTo[List[RootToken]],
            integrations = f("integrations").convertTo[List[Integration]],
            seats = f("seats").convertTo[List[Seat]],
            allowQueries = f("allowQueries").convertTo[Boolean],
            allowMutations = f("allowMutations").convertTo[Boolean],
            packageDefinitions = f("packageDefinitions").convertTo[List[PackageDefinition]],
            functions = f("functions").convertTo[List[Function]],
            featureToggles = f("featureToggles").convertTo[List[FeatureToggle]],
            typePositions = f("typePositions").convertTo[List[String]],
            isEjected = f("isEjected").convertTo[Boolean],
            hasGlobalStarPermission = f("hasGlobalStarPermission").convertTo[Boolean]
          )
        } catch {
          case e: Throwable => sys.error("Couldn't parse Project: " + e.getMessage)
        }
      }
    }

    implicit val clientFormat: RootJsonFormat[Client] = jsonFormat11(Client.apply)
    implicit val projectWithClientIdFormat            = jsonFormat(ProjectWithClientId.apply, "project", "clientId")
  }

  def serialize(projectWithClientId: ProjectWithClientId): String = {
    import CaseClassFormats._

    projectWithClientId.toJson.compactPrint
  }

  def serialize(project: Project): String = {
    import CaseClassFormats._

    project.toJson.compactPrint
  }

  def deserializeProjectWithClientId(string: String): Try[ProjectWithClientId] = {
    import CaseClassFormats._

    Try(string.parseJson.convertTo[ProjectWithClientId])
  }

  def deserializeProject(string: String): Try[Project] = {
    import CaseClassFormats._

    Try(string.parseJson.convertTo[Project])
  }
}
