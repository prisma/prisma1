package cool.graph.shared.models

import cool.graph.cuid.Cuid
import cool.graph.shared.gc_values.GCValue
import cool.graph.shared.models.ActionTriggerMutationModelMutationType.ActionTriggerMutationModelMutationType
import cool.graph.shared.models.CustomRule.CustomRule
import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import cool.graph.shared.models.IntegrationName.IntegrationName
import cool.graph.shared.models.IntegrationType.IntegrationType
import cool.graph.shared.models.LogStatus.LogStatus
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.ModelOperation.ModelOperation
import cool.graph.shared.models.Region.Region
import cool.graph.shared.models.SeatStatus.SeatStatus
import cool.graph.shared.models.UserType.UserType
import org.joda.time.DateTime

/**
  * BEGIN NEW STUFF
  * --------------------------------------------------------
  */
case class MigrationSteps(
    steps: Vector[MigrationStep]
)

trait MigrationStep
trait ModelMigrationStep                              extends MigrationStep
case class CreateModel(name: String)                  extends ModelMigrationStep
case class UpdateModel(name: String, newName: String) extends ModelMigrationStep
case class DeleteModel(name: String)                  extends ModelMigrationStep

trait FieldMigrationStep                                                         extends MigrationStep
case class CreateField(model: String, name: String)                              extends FieldMigrationStep
case class UpdateField(model: String, name: String, isRequired: Option[Boolean]) extends FieldMigrationStep
case class DeleteField(model: String, name: String)                              extends FieldMigrationStep

/**
  * END NEW STUFF
  * --------------------------------------------------------
  */
object IdType {
  type Id = String
}

import cool.graph.shared.models.IdType._

object CustomerSource extends Enumeration {
  type CustomerSource = Value
  val LEARN_RELAY  = Value("LEARN_RELAY")
  val LEARN_APOLLO = Value("LEARN_APOLLO")
  val DOCS         = Value("DOCS")
  val WAIT_LIST    = Value("WAIT_LIST")
  val HOMEPAGE     = Value("HOMEPAGE")
}

object MutationLogStatus extends Enumeration {
  type MutationLogStatus = Value
  val SCHEDULED  = Value("SCHEDULED")
  val SUCCESS    = Value("SUCCESS")
  val FAILURE    = Value("FAILURE")
  val ROLLEDBACK = Value("ROLLEDBACK")
}

case class Client(
    id: Id,
    auth0Id: Option[String] = None,
    isAuth0IdentityProviderEmail: Boolean = false,
    name: String,
    email: String,
    hashedPassword: String,
    resetPasswordSecret: Option[String] = None,
    source: CustomerSource.Value,
    projects: List[Project] = List(),
    createdAt: DateTime,
    updatedAt: DateTime
)

object SeatStatus extends Enumeration {
  type SeatStatus = Value
  val JOINED               = Value("JOINED")
  val INVITED_TO_PROJECT   = Value("INVITED_TO_PROJECT")
  val INVITED_TO_GRAPHCOOL = Value("INVITED_TO_GRAPHCOOL")
}

object Region extends Enumeration {
  type Region = Value
  val EU_WEST_1      = Value("eu-west-1")
  val US_WEST_2      = Value("us-west-2")
  val AP_NORTHEAST_1 = Value("ap-northeast-1")
}

case class Seat(id: String, status: SeatStatus, isOwner: Boolean, email: String, clientId: Option[String], name: Option[String])

case class PackageDefinition(
    id: Id,
    name: String,
    definition: String,
    formatVersion: Int
)

object LogStatus extends Enumeration {
  type LogStatus = Value
  val SUCCESS = Value("SUCCESS")
  val FAILURE = Value("FAILURE")
}

object RequestPipelineOperation extends Enumeration {
  type RequestPipelineOperation = Value
  val CREATE = Value("CREATE")
  val UPDATE = Value("UPDATE")
  val DELETE = Value("DELETE")
}

case class Log(
    id: Id,
    requestId: Option[String],
    status: LogStatus,
    duration: Int,
    timestamp: DateTime,
    message: String
)

sealed trait Function
sealed trait ServerSideSubscriptionFunction extends Function

case class Project(
    id: Id,
    name: String,
    projectDatabase: ProjectDatabase,
    ownerId: Id,
    alias: Option[String] = None,
    revision: Int = 1,
    webhookUrl: Option[String] = None,
    models: List[Model] = List.empty,
    relations: List[Relation] = List.empty,
    enums: List[Enum] = List.empty,
    actions: List[Action] = List.empty,
    rootTokens: List[RootToken] = List.empty,
    integrations: List[Integration] = List.empty,
    seats: List[Seat] = List.empty,
    allowQueries: Boolean = true,
    allowMutations: Boolean = true,
    packageDefinitions: List[PackageDefinition] = List.empty,
    functions: List[Function] = List.empty,
    featureToggles: List[FeatureToggle] = List.empty,
    typePositions: List[Id] = List.empty,
    isEjected: Boolean = false,
    hasGlobalStarPermission: Boolean = false
) {

  def actionsFor(modelId: Id, trigger: ActionTriggerMutationModelMutationType): List[Action] = {
    this.actions.filter { action =>
      action.isActive &&
      action.triggerMutationModel.exists(_.modelId == modelId) &&
      action.triggerMutationModel.exists(_.mutationType == trigger)
    }
  }

  def serverSideSubscriptionFunctionsFor(model: Model, mutationType: ModelMutationType): Seq[ServerSideSubscriptionFunction] = {
    ???
  }

  def hasEnabledAuthProvider: Boolean   = authProviders.exists(_.isEnabled)
  def authProviders: List[AuthProvider] = integrations.collect { case authProvider: AuthProvider => authProvider }

  def searchProviderAlgolia: Option[SearchProviderAlgolia] = {
    integrations
      .collect { case searchProviderAlgolia: SearchProviderAlgolia => searchProviderAlgolia }
      .find(_.name == IntegrationName.SearchProviderAlgolia)
  }

  def getAuthProviderById(id: Id): Option[AuthProvider] = authProviders.find(_.id == id)
  def getAuthProviderById_!(id: Id): AuthProvider       = ???

  def getServerSideSubscriptionFunction(id: Id): Option[ServerSideSubscriptionFunction] = ???
  def getServerSideSubscriptionFunction_!(id: Id): ServerSideSubscriptionFunction       = ???

  def getFunctionById(id: Id): Option[Function] = ???
  def getFunctionById_!(id: Id): Function       = ???

  def getFunctionByName(name: String): Option[Function] = ???
  def getFunctionByName_!(name: String): Function       = ???

  def getModelById(id: Id): Option[Model] = models.find(_.id == id)
  def getModelById_!(id: Id): Model       = ???

  def getModelByModelPermissionId(id: Id): Option[Model] = models.find(_.permissions.exists(_.id == id))
  def getModelByModelPermissionId_!(id: Id): Model       = ???

  def getRelationByRelationPermissionId(id: Id): Option[Relation] = relations.find(_.permissions.exists(_.id == id))
  def getRelationByRelationPermissionId_!(id: Id): Relation       = ???

  def getActionById(id: Id): Option[Action] = actions.find(_.id == id)
  def getActionById_!(id: Id): Action       = ???

  def getRootTokenById(id: String): Option[RootToken] = rootTokens.find(_.id == id)
  def getRootTokenById_!(id: String): RootToken       = ???

  def getRootTokenByName(name: String): Option[RootToken] = rootTokens.find(_.name == name)
  def getRootTokenByName_!(name: String): RootToken       = ???

  // note: mysql columns are case insensitive, so we have to be as well. But we could make them case sensitive https://dev.mysql.com/doc/refman/5.6/en/case-sensitivity.html
  def getModelByName(name: String): Option[Model] = models.find(_.name.toLowerCase() == name.toLowerCase())
  def getModelByName_!(name: String): Model       = ???

  def getModelByFieldId(id: Id): Option[Model] = models.find(_.fields.exists(_.id == id))
  def getModelByFieldId_!(id: Id): Model       = ???

  def getFieldById(id: Id): Option[Field] = models.flatMap(_.fields).find(_.id == id)
  def getFieldById_!(id: Id): Field       = ???

  def getFieldConstraintById(id: Id): Option[FieldConstraint] = {
    val fields      = models.flatMap(_.fields)
    val constraints = fields.flatMap(_.constraints)
    constraints.find(_.id == id)
  }
  def getFieldConstraintById_!(id: Id): FieldConstraint = ???

  def getEnumById(enumId: String): Option[Enum] = enums.find(_.id == enumId)
  def getEnumById_!(enumId: String): Enum       = ???

  // note: mysql columns are case insensitive, so we have to be as well
  def getEnumByName(name: String): Option[Enum] = enums.find(_.name.toLowerCase == name.toLowerCase)

  def getRelationById(id: Id): Option[Relation] = relations.find(_.id == id)
  def getRelationById_!(id: Id): Relation       = ???

  def getRelationByName(name: String): Option[Relation] = relations.find(_.name == name)
  def getRelationByName_!(name: String): Relation       = ???

  def getRelationFieldMirrorById(id: Id): Option[RelationFieldMirror] = relations.flatMap(_.fieldMirrors).find(_.id == id)

  def getFieldByRelationFieldMirrorId(id: Id): Option[Field] = getRelationFieldMirrorById(id).flatMap(mirror => getFieldById(mirror.fieldId))
  def getFieldByRelationFieldMirrorId_!(id: Id): Field       = ???

  def getRelationByFieldMirrorId(id: Id): Option[Relation] = relations.find(_.fieldMirrors.exists(_.id == id))
  def getRelationByFieldMirrorId_!(id: Id): Relation       = ???

  def getIntegrationByTypeAndName(integrationType: IntegrationType, name: IntegrationName): Option[Integration] = {
    integrations.filter(_.integrationType == integrationType).find(_.name == name)
  }

  def getSearchProviderAlgoliaById(id: Id): Option[SearchProviderAlgolia] = {
    authProviders
      .map(_.metaInformation)
      .collect { case Some(metaInfo: SearchProviderAlgolia) => metaInfo }
      .find(_.id == id)
  }

  def getSearchProviderAlgoliaByAlgoliaSyncQueryId_!(id: Id): SearchProviderAlgolia = ???

  def getSearchProviderAlgoliaByAlgoliaSyncQueryId(id: Id): Option[SearchProviderAlgolia] = {
    integrations
      .collect { case searchProviderAlgolia: SearchProviderAlgolia => searchProviderAlgolia }
      .find(_.algoliaSyncQueries.exists(_.id == id))
  }

  def getAlgoliaSyncQueryById_!(id: Id): AlgoliaSyncQuery = ???

  def getAlgoliaSyncQueryById(id: Id): Option[AlgoliaSyncQuery] = {
    integrations
      .collect { case searchProviderAlgolia: SearchProviderAlgolia => searchProviderAlgolia }
      .flatMap(_.algoliaSyncQueries)
      .find(_.id == id)
  }

  def getFieldsByRelationId(id: Id): List[Field] = models.flatMap(_.fields).filter(f => f.relation.isDefined && f.relation.get.id == id)

  def getRelationFieldMirrorsByFieldId(id: Id): List[RelationFieldMirror] = relations.flatMap(_.fieldMirrors).filter(f => f.fieldId == id)

  lazy val getOneRelations: List[Relation] = {
    relations.filter(
      relation =>
        !relation.getModelAField(this).exists(_.isList) &&
          !relation.getModelBField(this).exists(_.isList))
  }

  lazy val getManyRelations: List[Relation] = relations.filter(x => !getOneRelations.contains(x))

  def getRelatedModelForField(field: Field): Option[Model] = {
    val relation = field.relation.getOrElse {
      return None
    }

    val modelId = field.relationSide match {
      case Some(side) if side == RelationSide.A => Some(relation.modelBId)
      case Some(side) if side == RelationSide.B => Some(relation.modelAId)
      case _                                    => None
    }

    modelId.flatMap(id => getModelById(id))
  }

  def getReverseRelationField(field: Field): Option[Field] = {
    val relation     = field.relation.getOrElse { return None }
    val relationSide = field.relationSide.getOrElse { return None }

    val relatedModelId = relationSide match {
      case RelationSide.A => relation.modelBId
      case RelationSide.B => relation.modelAId
    }

    val relatedModel = getModelById_!(relatedModelId)

    relatedModel.fields.find(
      relatedField =>
        relatedField.relation
          .contains(relation) && relatedField.id != field.id) match {
      case Some(relatedField) => Some(relatedField)
      case None               => relatedModel.fields.find(relatedField => relatedField.relation.contains(relation))
    }

  }

  def seatByEmail(email: String): Option[Seat] = seats.find(_.email == email)
  def seatByEmail_!(email: String): Seat       = ???

  def seatByClientId(clientId: Id): Option[Seat] = seats.find(_.clientId.contains(clientId))
  def seatByClientId_!(clientId: Id): Seat       = ???

  def getModelPermissionById(id: Id): Option[ModelPermission] = models.flatMap(_.permissions).find(_.id == id)
  def getModelPermissionById_!(id: Id): ModelPermission       = ???

  def getRelationPermissionById(id: Id): Option[RelationPermission] = relations.flatMap(_.permissions).find(_.id == id)
  def getRelationPermissionById_!(id: Id): RelationPermission       = ???

  def modelPermissions: List[ModelPermission]      = models.flatMap(_.permissions)
  def relationPermissions: Seq[RelationPermission] = relations.flatMap(_.permissions)

  def relationPermissionByRelationPermissionId(id: Id): Option[RelationPermission] = relations.flatMap(_.permissions).find(_.id == id)
  def relationPermissionByRelationPermissionId_!(id: Id): RelationPermission       = ???

  def relationByRelationPermissionId(id: Id): Option[Relation] = relations.find(_.permissions.exists(_.id == id))
  def relationByRelationPermissionId_!(id: Id): Relation       = ???

  def allFields: Seq[Field] = models.flatMap(_.fields)

  def hasSchemaNameConflict(name: String, id: String): Boolean = ???
}

case class ProjectWithClientId(project: Project, clientId: Id) {
  val id: Id = project.id
}
case class ProjectWithClient(project: Project, client: Client)

case class ProjectDatabase(id: Id, region: Region, name: String, isDefaultForRegion: Boolean = false)

trait AuthProviderMetaInformation {
  val id: String
}

case class AuthProviderDigits(
    id: String,
    consumerKey: String,
    consumerSecret: String
) extends AuthProviderMetaInformation

case class AuthProviderAuth0(
    id: String,
    domain: String,
    clientId: String,
    clientSecret: String
) extends AuthProviderMetaInformation

case class SearchProviderAlgolia(
    id: String,
    subTableId: String,
    applicationId: String,
    apiKey: String,
    algoliaSyncQueries: List[AlgoliaSyncQuery] = List(),
    isEnabled: Boolean,
    name: IntegrationName
) extends Integration {
  override val integrationType: IntegrationType = IntegrationType.SearchProvider
}

case class AlgoliaSyncQuery(
    id: String,
    indexName: String,
    fragment: String,
    isEnabled: Boolean,
    model: Model
)

sealed trait AuthenticatedRequest {
  def id: String
  def originalToken: String
  val isAdmin: Boolean = this match {
    case _: AuthenticatedCustomer  => true
    case _: AuthenticatedRootToken => true
    case _: AuthenticatedUser      => false
  }
}

case class AuthenticatedUser(id: String, typeName: String, originalToken: String) extends AuthenticatedRequest
case class AuthenticatedCustomer(id: String, originalToken: String)               extends AuthenticatedRequest
case class AuthenticatedRootToken(id: String, originalToken: String)              extends AuthenticatedRequest

object IntegrationType extends Enumeration {
  type IntegrationType = Value
  val AuthProvider   = Value("AUTH_PROVIDER")
  val SearchProvider = Value("SEARCH_PROVIDER")
}

object IntegrationName extends Enumeration {
  type IntegrationName = Value
  val AuthProviderAuth0     = Value("AUTH_PROVIDER_AUTH0")
  val AuthProviderDigits    = Value("AUTH_PROVIDER_DIGITS")
  val AuthProviderEmail     = Value("AUTH_PROVIDER_EMAIL")
  val SearchProviderAlgolia = Value("SEARCH_PROVIDER_ALGOLIA")
}

case class AuthProvider(
    id: String,
    subTableId: String = "this-should-be-set-explicitly",
    isEnabled: Boolean,
    name: IntegrationName.IntegrationName, // note: this defines the meta table name
    metaInformation: Option[AuthProviderMetaInformation]
) extends Integration {
  override val integrationType = IntegrationType.AuthProvider
}

trait Integration {
  val id: String
  val subTableId: String
  val isEnabled: Boolean
  val integrationType: IntegrationType.IntegrationType
  val name: IntegrationName.IntegrationName
}

case class ModelPermission(
    id: Id,
    operation: ModelOperation,
    userType: UserType,
    rule: CustomRule = CustomRule.None,
    ruleName: Option[String] = None,
    ruleGraphQuery: Option[String] = None,
    ruleGraphQueryFilePath: Option[String] = None,
    ruleWebhookUrl: Option[String] = None,
    fieldIds: List[String] = List(),
    applyToWholeModel: Boolean,
    description: Option[String] = None,
    isActive: Boolean
) {
  def isCustom: Boolean = rule != CustomRule.None

  def isNotCustom: Boolean = !isCustom

  def operationString = operation match {
    case ModelOperation.Create => "create"
    case ModelOperation.Read   => "read"
    case ModelOperation.Update => "update"
    case ModelOperation.Delete => "delete"
  }
}

object ModelPermission {
  def publicPermissions: List[ModelPermission] =
    List(ModelOperation.Read, ModelOperation.Create, ModelOperation.Update, ModelOperation.Delete)
      .map(
        operation =>
          ModelPermission(
            id = Cuid.createCuid(),
            operation = operation,
            userType = UserType.Everyone,
            rule = CustomRule.None,
            ruleName = None,
            ruleGraphQuery = None,
            ruleWebhookUrl = None,
            isActive = true,
            fieldIds = List.empty,
            applyToWholeModel = true
        ))

  def authenticatedPermissions: List[ModelPermission] =
    List(ModelOperation.Read, ModelOperation.Create, ModelOperation.Update, ModelOperation.Delete)
      .map(
        operation =>
          ModelPermission(
            id = Cuid.createCuid(),
            operation = operation,
            userType = UserType.Authenticated,
            rule = CustomRule.None,
            ruleName = None,
            ruleGraphQuery = None,
            ruleWebhookUrl = None,
            isActive = true,
            fieldIds = List.empty,
            applyToWholeModel = true
        ))
}

case class RelationPermission(
    id: Id,
    connect: Boolean,
    disconnect: Boolean,
    userType: UserType,
    rule: CustomRule = CustomRule.None,
    ruleName: Option[String] = None,
    ruleGraphQuery: Option[String] = None,
    ruleGraphQueryFilePath: Option[String] = None,
    ruleWebhookUrl: Option[String] = None,
    description: Option[String] = None,
    isActive: Boolean
) {
  def isCustom: Boolean = rule != CustomRule.None

  def isNotCustom: Boolean = !isCustom

  def operation = (connect, disconnect) match {
    case (true, false)  => "connect"
    case (false, true)  => "disconnect"
    case (true, true)   => "*"
    case (false, false) => "none"
  }

  def operationString = (connect, disconnect) match {
    case (true, false)  => "connect"
    case (false, true)  => "disconnect"
    case (true, true)   => "connectAndDisconnect"
    case (false, false) => "none"
  }

}

object RelationPermission {
  def publicPermissions =
    List(
      RelationPermission(
        id = Cuid.createCuid(),
        connect = true,
        disconnect = true,
        userType = UserType.Everyone,
        rule = CustomRule.None,
        ruleName = None,
        ruleGraphQuery = None,
        ruleWebhookUrl = None,
        isActive = true
      ))
}

case class Model(
    id: Id,
    name: String,
    description: Option[String] = None,
    isSystem: Boolean,
    fields: List[Field] = List.empty,
    permissions: List[ModelPermission] = List.empty,
    fieldPositions: List[Id] = List.empty
) {

  lazy val scalarFields: List[Field]         = fields.filter(_.isScalar)
  lazy val relationFields: List[Field]       = fields.filter(_.isRelation)
  lazy val singleRelationFields: List[Field] = relationFields.filter(!_.isList)
  lazy val listRelationFields: List[Field]   = relationFields.filter(_.isList)

  def relationFieldForIdAndSide(relationId: String, relationSide: RelationSide.Value): Option[Field] = {
    fields.find(_.isRelationWithIdAndSide(relationId, relationSide))
  }

  lazy val relations: List[Relation] = {
    fields
      .map(_.relation)
      .collect { case Some(relation) => relation }
      .distinct
  }

  def withoutFieldsForRelation(relation: Relation): Model = withoutFieldsForRelations(Seq(relation))

  def withoutFieldsForRelations(relations: Seq[Relation]): Model = {
    val newFields = for {
      field <- fields
      if relations.forall(relation => !field.isRelationWithId(relation.id))
    } yield field
    copy(fields = newFields)
  }

  def filterFields(fn: Field => Boolean): Model = copy(fields = this.fields.filter(fn))

  def getFieldById_!(id: Id): Field       = ???
  def getFieldById(id: Id): Option[Field] = fields.find(_.id == id)

  def getFieldByName_!(name: String): Field       = ??? //getFieldByName(name).getOrElse(throw FieldNotInModel(fieldName = name, modelName = this.name))
  def getFieldByName(name: String): Option[Field] = fields.find(_.name == name)

  def getPermissionById(id: Id): Option[ModelPermission] = permissions.find(_.id == id)

  lazy val getCamelCasedName: String = Character.toLowerCase(name.charAt(0)) + name.substring(1)
  lazy val isUserModel: Boolean      = name == "User"

  lazy val hasQueryPermissions: Boolean = permissions.exists(permission => permission.isCustom && permission.isActive)
}

object RelationSide extends Enumeration {
  type RelationSide = Value
  val A = Value("A")
  val B = Value("B")
}

object TypeIdentifier extends Enumeration {
  // note: casing of values are chosen to match our TypeIdentifiers
  type TypeIdentifier = Value
  val String    = Value("String")
  val Int       = Value("Int")
  val Float     = Value("Float")
  val Boolean   = Value("Boolean")
  val Password  = Value("Password")
  val DateTime  = Value("DateTime")
  val GraphQLID = Value("GraphQLID")
  val Enum      = Value("Enum")
  val Json      = Value("Json")
  val Relation  = Value("Relation")

  def withNameOpt(name: String): Option[TypeIdentifier.Value] = this.values.find(_.toString == name)
}

case class Enum(
    id: Id,
    name: String,
    values: Seq[String] = Seq.empty
)

case class FeatureToggle(
    id: Id,
    name: String,
    isEnabled: Boolean
)

case class Field(
    id: Id,
    name: String,
    typeIdentifier: TypeIdentifier.Value,
    description: Option[String] = None,
    isRequired: Boolean,
    isList: Boolean,
    isUnique: Boolean,
    isSystem: Boolean,
    isReadonly: Boolean,
    enum: Option[Enum] = None,
    defaultValue: Option[GCValue] = None,
    relation: Option[Relation] = None,
    relationSide: Option[RelationSide.Value] = None,
    constraints: List[FieldConstraint] = List.empty
) {

  def isScalar: Boolean                             = ???
  def isRelation: Boolean                           = typeIdentifier == TypeIdentifier.Relation
  def isRelationWithId(relationId: String): Boolean = relation.exists(_.id == relationId)

  def isRelationWithIdAndSide(relationId: String, relationSide: RelationSide.Value): Boolean = {
    isRelationWithId(relationId) && this.relationSide.contains(relationSide)
  }

  def isWritable: Boolean = !isReadonly

  def isOneToOneRelation(project: Project): Boolean = {
    val otherField = relatedFieldEager(project)
    !this.isList && !otherField.isList
  }

  def isManyToManyRelation(project: Project): Boolean = {
    val otherField = relatedFieldEager(project)
    this.isList && otherField.isList
  }

  def isOneToManyRelation(project: Project): Boolean = {
    val otherField = relatedFieldEager(project)
    (this.isList && !otherField.isList) || (!this.isList && otherField.isList)
  }

  def oppositeRelationSide: Option[RelationSide.Value] = {
    relationSide match {
      case Some(RelationSide.A) => Some(RelationSide.B)
      case Some(RelationSide.B) => Some(RelationSide.A)
      case x                    => ??? //throw SystemErrors.InvalidStateException(message = s" relationSide was $x")
    }
  }

  def relatedModel_!(project: Project): Model = {
    relatedModel(project) match {
      case None        => sys.error(s"Could not find relatedModel for field [$name] on model [${model(project)}]")
      case Some(model) => model
    }
  }

  def relatedModel(project: Project): Option[Model] = {
    relation.flatMap(relation => {
      relationSide match {
        case Some(RelationSide.A) => relation.getModelB(project)
        case Some(RelationSide.B) => relation.getModelA(project)
        case x                    => ??? //throw SystemErrors.InvalidStateException(message = s" relationSide was $x")
      }
    })
  }

  def model(project: Project): Option[Model] = {
    relation.flatMap(relation => {
      relationSide match {
        case Some(RelationSide.A) => relation.getModelA(project)
        case Some(RelationSide.B) => relation.getModelB(project)
        case x                    => ??? //throw SystemErrors.InvalidStateException(message = s" relationSide was $x")
      }
    })
  }

  def relatedFieldEager(project: Project): Field = {
    val fields = relatedModel(project).get.fields

    var returnField = fields.find { field =>
      field.relation.exists { relation =>
        val isTheSameField    = field.id == this.id
        val isTheSameRelation = relation.id == this.relation.get.id
        isTheSameRelation && !isTheSameField
      }
    }

    if (returnField.isEmpty) {
      returnField = fields.find { relatedField =>
        relatedField.relation.exists { relation =>
          relation.id == this.relation.get.id
        }
      }
    }
    returnField.head
  }
}

sealed trait FieldConstraint {
  val id: String; val fieldId: String; val constraintType: FieldConstraintType
}

case class StringConstraint(id: String,
                            fieldId: String,
                            equalsString: Option[String] = None,
                            oneOfString: List[String] = List.empty,
                            minLength: Option[Int] = None,
                            maxLength: Option[Int] = None,
                            startsWith: Option[String] = None,
                            endsWith: Option[String] = None,
                            includes: Option[String] = None,
                            regex: Option[String] = None)
    extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.STRING
}

case class NumberConstraint(id: String,
                            fieldId: String,
                            equalsNumber: Option[Double] = None,
                            oneOfNumber: List[Double] = List.empty,
                            min: Option[Double] = None,
                            max: Option[Double] = None,
                            exclusiveMin: Option[Double] = None,
                            exclusiveMax: Option[Double] = None,
                            multipleOf: Option[Double] = None)
    extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.NUMBER
}

case class BooleanConstraint(id: String, fieldId: String, equalsBoolean: Option[Boolean] = None) extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.BOOLEAN
}

case class ListConstraint(id: String, fieldId: String, uniqueItems: Option[Boolean] = None, minItems: Option[Int] = None, maxItems: Option[Int] = None)
    extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.LIST
}

object FieldConstraintType extends Enumeration {
  type FieldConstraintType = Value
  val STRING  = Value("STRING")
  val NUMBER  = Value("NUMBER")
  val BOOLEAN = Value("BOOLEAN")
  val LIST    = Value("LIST")
}

// NOTE modelA/modelB should actually be included here
// but left out for now because of cyclic dependencies
case class Relation(
    id: Id,
    name: String,
    description: Option[String] = None,
    // BEWARE: if the relation looks like this: val relation = Relation(id = "relationId", modelAId = "userId", modelBId = "todoId")
    // then the relationSide for the fields have to be "opposite", because the field's side is the side of _the other_ model
    // val userField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.B)
    // val todoField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.A)
    modelAId: Id,
    modelBId: Id,
    fieldMirrors: List[RelationFieldMirror] = List(),
    permissions: List[RelationPermission] = List()
) {
  def connectsTheModels(model1: Model, model2: Model): Boolean = {
    (modelAId == model1.id && modelBId == model2.id) || (modelAId == model2.id && modelBId == model1.id)
  }

  def isSameModelRelation(project: Project): Boolean          = getModelA(project) == getModelB(project)
  def isSameFieldSameModelRelation(project: Project): Boolean = getModelAField(project) == getModelBField(project)

  def getModelA(project: Project): Option[Model] = project.getModelById(modelAId)
  def getModelA_!(project: Project): Model       = ??? //getModelA(project).getOrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))

  def getModelB(project: Project): Option[Model] = project.getModelById(modelBId)
  def getModelB_!(project: Project): Model       = ??? //getModelB(project).getOrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))

  def getOtherModel_!(project: Project, model: Model): Model = {
    model.id match {
      case `modelAId` => getModelB_!(project)
      case `modelBId` => getModelA_!(project)
      case _          => ??? //throw SystemErrors.InvalidRelation(s"The model with the id ${model.id} is not part of this relation.")
    }
  }

  def fields(project: Project): Iterable[Field] = getModelAField(project) ++ getModelBField(project)

  def getOtherField_!(project: Project, model: Model): Field = {
    model.id match {
      case `modelAId` => getModelBField_!(project)
      case `modelBId` => getModelAField_!(project)
      case _          => ??? //throw SystemErrors.InvalidRelation(s"The model with the id ${model.id} is not part of this relation.")
    }
  }

  def getModelAField(project: Project): Option[Field] = modelFieldFor(project, modelAId, RelationSide.A)
  def getModelAField_!(project: Project): Field =
    ??? //getModelAField(project).getOrElse(throw SystemErrors.InvalidRelation("A relation must have a field on model A."))

  def getModelBField(project: Project): Option[Field] = {
    // note: defaults to modelAField to handle same model, same field relations
    modelFieldFor(project, modelBId, RelationSide.B).orElse(getModelAField(project))
  }
  def getModelBField_!(project: Project): Field =
    ??? //getModelBField(project).getOrElse(throw SystemErrors.InvalidRelation("This must return a Model, if not Model B then Model A."))

  private def modelFieldFor(project: Project, modelId: String, relationSide: RelationSide.Value): Option[Field] = {
    for {
      model <- project.getModelById(modelId)
      field <- model.relationFieldForIdAndSide(relationId = id, relationSide = relationSide)
    } yield field
  }

  def aName(project: Project): String =
    getModelAField(project)
      .map(field => s"${field.name}${makeUnique("1", project)}${field.relatedModel(project).get.name}")
      .getOrElse("from")

  def bName(project: Project): String =
    getModelBField(project)
      .map(field => s"${field.name}${makeUnique("2", project)}${field.relatedModel(project).get.name}")
      .getOrElse("to")

  private def makeUnique(x: String, project: Project) = if (getModelAField(project) == getModelBField(project)) x else ""

  def fieldSide(project: Project, field: Field): cool.graph.shared.models.RelationSide.Value = {
    val fieldModel = project.getModelByFieldId_!(field.id)
    fieldModel.id match {
      case `modelAId` => RelationSide.A
      case `modelBId` => RelationSide.B
    }
  }

  def getPermissionById(id: String): Option[RelationPermission] = permissions.find(_.id == id)

  def getRelationFieldMirrorById(id: String): Option[RelationFieldMirror] = fieldMirrors.find(_.id == id)
  def getRelationFieldMirrorById_!(id: String): RelationFieldMirror =
    ??? //getRelationFieldMirrorById(id).getOrElse(throw SystemErrors.InvalidRelationFieldMirrorId(id))

}

case class RelationFieldMirror(
    id: String,
    relationId: String,
    fieldId: String
)

object UserType extends Enumeration {
  type UserType = Value
  val Everyone      = Value("EVERYONE")
  val Authenticated = Value("AUTHENTICATED")
}

object ModelMutationType extends Enumeration {
  type ModelMutationType = Value
  val Created = Value("CREATED")
  val Updated = Value("UPDATED")
  val Deleted = Value("DELETED")
}

object CustomRule extends Enumeration {
  type CustomRule = Value
  val None    = Value("NONE")
  val Graph   = Value("GRAPH")
  val Webhook = Value("WEBHOOK")
}

object ModelOperation extends Enumeration {
  type ModelOperation = Value
  val Create = Value("CREATE")
  val Read   = Value("READ")
  val Update = Value("UPDATE")
  val Delete = Value("DELETE")
}

case class RootToken(id: Id, token: String, name: String, created: DateTime)

object ActionTriggerType extends Enumeration {
  type ActionTriggerType = Value
  val MutationModel    = Value("MUTATION_MODEL")
  val MutationRelation = Value("MUTATION_RELATION")
}

object ActionHandlerType extends Enumeration {
  type ActionHandlerType = Value
  val Webhook = Value("WEBHOOK")
}

case class Action(
    id: Id,
    isActive: Boolean,
    triggerType: ActionTriggerType.Value,
    handlerType: ActionHandlerType.Value,
    description: Option[String] = None,
    handlerWebhook: Option[ActionHandlerWebhook] = None,
    triggerMutationModel: Option[ActionTriggerMutationModel] = None,
    triggerMutationRelation: Option[ActionTriggerMutationRelation] = None
)

case class ActionHandlerWebhook(
    id: Id,
    url: String,
    isAsync: Boolean
)

object ActionTriggerMutationModelMutationType extends Enumeration {
  type ActionTriggerMutationModelMutationType = Value
  val Create = Value("CREATE")
  val Update = Value("UPDATE")
  val Delete = Value("DELETE")
}

case class ActionTriggerMutationModel(
    id: Id,
    modelId: String,
    mutationType: ActionTriggerMutationModelMutationType.Value,
    fragment: String
)

object ActionTriggerMutationRelationMutationType extends Enumeration {
  type ActionTriggerMutationRelationMutationType = Value
  val Add    = Value("ADD")
  val Remove = Value("REMOVE")
}

case class ActionTriggerMutationRelation(
    id: Id,
    relationId: String,
    mutationType: ActionTriggerMutationRelationMutationType.Value,
    fragment: String
)
