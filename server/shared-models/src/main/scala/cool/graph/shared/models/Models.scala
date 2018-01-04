package cool.graph.shared.models

import cool.graph.gc_values.GCValue
import cool.graph.shared.errors.SharedErrors
import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import cool.graph.shared.models.LogStatus.LogStatus
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.SeatStatus.SeatStatus
import org.joda.time.DateTime

object IdType {
  type Id = String
}

import cool.graph.shared.models.IdType._

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
    projects: List[Project] = List.empty,
    createdAt: DateTime,
    updatedAt: DateTime
)

object SeatStatus extends Enumeration {
  type SeatStatus = Value
  val JOINED               = Value("JOINED")
  val INVITED_TO_PROJECT   = Value("INVITED_TO_PROJECT")
  val INVITED_TO_GRAPHCOOL = Value("INVITED_TO_GRAPHCOOL")
}

case class Seat(id: String, status: SeatStatus, isOwner: Boolean, email: String, clientId: Option[String], name: Option[String])

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

sealed trait Function {
  def id: Id
  def name: String
  def isActive: Boolean
//  def delivery: FunctionDelivery
//  def binding: FunctionBinding
}

case class ServerSideSubscriptionFunction(
    id: Id,
    name: String,
    isActive: Boolean,
    query: String,
    queryFilePath: Option[String] = None //,
//                                           delivery: FunctionDelivery
) extends Function {
//  def isServerSideSubscriptionFor(model: Model, mutationType: ModelMutationType): Boolean = {
//    val queryDoc             = QueryParser.parse(query).get
//    val modelNameInQuery     = QueryTransformer.getModelNameFromSubscription(queryDoc).get
//    val mutationTypesInQuery = QueryTransformer.getMutationTypesFromSubscription(queryDoc)
//    model.name == modelNameInQuery && mutationTypesInQuery.contains(mutationType)
//  }
//
//  def binding = FunctionBinding.SERVERSIDE_SUBSCRIPTION
}

case class Project(
    id: Id,
    ownerId: Id,
    revision: Int = 1,
    webhookUrl: Option[String] = None,
    models: List[Model] = List.empty,
    relations: List[Relation] = List.empty,
    enums: List[Enum] = List.empty,
    secrets: Vector[String] = Vector.empty,
    seats: List[Seat] = List.empty,
    allowQueries: Boolean = true,
    allowMutations: Boolean = true,
    functions: List[Function] = List.empty,
    featureToggles: List[FeatureToggle] = List.empty
) {

  lazy val projectId: ProjectId = ProjectId.fromEncodedString(id)

  val serverSideSubscriptionFunctions: List[ServerSideSubscriptionFunction] = functions.collect { case x: ServerSideSubscriptionFunction => x }

  def serverSideSubscriptionFunctionsFor(model: Model, mutationType: ModelMutationType): Seq[ServerSideSubscriptionFunction] = {
    serverSideSubscriptionFunctions
      .filter(_.isActive)
//      .filter(_.isServerSideSubscriptionFor(model, mutationType))
  }

  def getServerSideSubscriptionFunction(id: Id): Option[ServerSideSubscriptionFunction] = serverSideSubscriptionFunctions.find(_.id == id)
  def getServerSideSubscriptionFunction_!(id: Id): ServerSideSubscriptionFunction =
    getServerSideSubscriptionFunction(id).get //OrElse(throw SystemErrors.InvalidFunctionId(id))

  def getFunctionById(id: Id): Option[Function] = functions.find(_.id == id)
  def getFunctionById_!(id: Id): Function       = getFunctionById(id).get //OrElse(throw SystemErrors.InvalidFunctionId(id))

  def getFunctionByName(name: String): Option[Function] = functions.find(_.name == name)
  def getFunctionByName_!(name: String): Function       = getFunctionByName(name).get //OrElse(throw SystemErrors.InvalidFunctionName(name))

  def getModelById(id: Id): Option[Model] = models.find(_.id == id)
  def getModelById_!(id: Id): Model       = getModelById(id).getOrElse(throw SharedErrors.InvalidModel(id))

  // note: mysql columns are case insensitive, so we have to be as well. But we could make them case sensitive https://dev.mysql.com/doc/refman/5.6/en/case-sensitivity.html
  def getModelByName(name: String): Option[Model] = models.find(_.name.toLowerCase() == name.toLowerCase())
  def getModelByName_!(name: String): Model       = getModelByName(name).getOrElse(throw SharedErrors.InvalidModel(s"No model with name: $name found."))

  def getModelByFieldId(id: Id): Option[Model] = models.find(_.fields.exists(_.id == id))
  def getModelByFieldId_!(id: Id): Model       = getModelByFieldId(id).get //OrElse(throw SystemErrors.InvalidModel(s"No model with a field with id: $id found."))

  def getFieldById(id: Id): Option[Field]                        = models.flatMap(_.fields).find(_.id == id)
  def getFieldById_!(id: Id): Field                              = getFieldById(id).get //OrElse(throw SystemErrors.InvalidFieldId(id))
  def getFieldByName(model: String, name: String): Option[Field] = getModelByName(model).flatMap(_.getFieldByName(name))
  def getFieldByName_!(model: String, name: String): Field       = getModelByName_!(model).getFieldByName_!(name)

  def getFieldConstraintById(id: Id): Option[FieldConstraint] = {
    val fields      = models.flatMap(_.fields)
    val constraints = fields.flatMap(_.constraints)
    constraints.find(_.id == id)
  }
  def getFieldConstraintById_!(id: Id): FieldConstraint = getFieldConstraintById(id).get //OrElse(throw SystemErrors.InvalidFieldConstraintId(id))

  def getEnumById(enumId: String): Option[Enum] = enums.find(_.id == enumId)
  def getEnumById_!(enumId: String): Enum       = getEnumById(enumId).get //OrElse(throw SystemErrors.InvalidEnumId(id = enumId))

  // note: mysql columns are case insensitive, so we have to be as well
  def getEnumByName(name: String): Option[Enum] = enums.find(_.name.toLowerCase == name.toLowerCase)

  def getRelationById(id: Id): Option[Relation] = relations.find(_.id == id)
  def getRelationById_!(id: Id): Relation       = getRelationById(id).get //OrElse(throw SystemErrors.InvalidRelationId(id))

  def getRelationByName(name: String): Option[Relation] = relations.find(_.name == name)
  def getRelationByName_!(name: String): Relation =
    getRelationByName(name).get //OrElse(throw SystemErrors.InvalidRelation("There is no relation with name: " + name))

  def getRelationFieldMirrorById(id: Id): Option[RelationFieldMirror] = relations.flatMap(_.fieldMirrors).find(_.id == id)

  def getFieldByRelationFieldMirrorId(id: Id): Option[Field] = getRelationFieldMirrorById(id).flatMap(mirror => getFieldById(mirror.fieldId))
  def getFieldByRelationFieldMirrorId_!(id: Id): Field       = getFieldByRelationFieldMirrorId(id).get //OrElse(throw SystemErrors.InvalidRelationFieldMirrorId(id))

  def getRelationByFieldMirrorId(id: Id): Option[Relation] = relations.find(_.fieldMirrors.exists(_.id == id))
  def getRelationByFieldMirrorId_!(id: Id): Relation       = getRelationByFieldMirrorId(id).get //OrElse(throw SystemErrors.InvalidRelationFieldMirrorId(id))

  def getFieldsByRelationId(id: Id): List[Field] = models.flatMap(_.fields).filter(f => f.relation.isDefined && f.relation.get.id == id)

  def getUnambiguousRelationThatConnectsModels_!(modelA: String, modelB: String): Option[Relation] = {
    val candidates = relations.filter(_.connectsTheModels(modelA, modelB))
    require(candidates.size < 2, "This method must only be called for unambiguous relations!")
    candidates.headOption
  }

  def getRelationFieldMirrorsByFieldId(id: Id): List[RelationFieldMirror] = relations.flatMap(_.fieldMirrors).filter(f => f.fieldId == id)

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
  def seatByEmail_!(email: String): Seat       = seatByEmail(email).get //OrElse(throw SystemErrors.InvalidSeatEmail(email))

  def seatByClientId(clientId: Id): Option[Seat] = seats.find(_.clientId.contains(clientId))
  def seatByClientId_!(clientId: Id): Seat       = seatByClientId(clientId).get //OrElse(throw SystemErrors.InvalidSeatClientId(clientId))

  def allFields: Seq[Field] = models.flatMap(_.fields)

  def hasSchemaNameConflict(name: String, id: String): Boolean = {
    val conflictingType = this.models.exists(model => List(s"create${model.name}", s"update${model.name}", s"delete${model.name}").contains(name))
    conflictingType
  }
}

case class ProjectWithClientId(project: Project, clientId: Id) {
  val id: Id = project.id
}
case class ProjectWithClient(project: Project, client: Client)

case class Model(
    id: Id,
    name: String,
    fields: List[Field],
    description: Option[String] = None
) {

  lazy val scalarFields: List[Field]         = fields.filter(_.isScalar)
  lazy val scalarListFields: List[Field]     = scalarFields.filter(_.isList)
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

  def getFieldById_!(id: Id): Field       = getFieldById(id).get
  def getFieldById(id: Id): Option[Field] = fields.find(_.id == id)

  def getFieldByName_!(name: String): Field =
    getFieldByName(name).getOrElse(sys.error(s"field $name is not part of the model $name")) // .getOrElse(throw FieldNotInModel(fieldName = name, modelName = this.name))
  def getFieldByName(name: String): Option[Field] = fields.find(_.name == name)

  def hasVisibleIdField: Boolean = getFieldByName_!("id").isVisible
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
  val DateTime  = Value("DateTime")
  val GraphQLID = Value("GraphQLID")
  val Enum      = Value("Enum")
  val Json      = Value("Json")
  val Relation  = Value("Relation")

  def withNameOpt(name: String): Option[TypeIdentifier.Value] = name match {
    case "ID" => Some(GraphQLID)
    case _    => this.values.find(_.toString == name)
  }

  def withNameHacked(name: String) = name match {
    case "ID" => GraphQLID
    case _    => withName(name)
  }
}

case class Enum(
    id: Id,
    name: String,
    values: Vector[String] = Vector.empty
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
    isHidden: Boolean = false,
    isReadonly: Boolean = false,
    enum: Option[Enum],
    defaultValue: Option[GCValue],
    relation: Option[Relation],
    relationSide: Option[RelationSide.Value],
    constraints: List[FieldConstraint] = List.empty
) {

  def isScalar: Boolean                             = typeIdentifier != TypeIdentifier.Relation
  def isRelation: Boolean                           = typeIdentifier == TypeIdentifier.Relation
  def isRelationWithId(relationId: String): Boolean = relation.exists(_.id == relationId)

  def isRelationWithIdAndSide(relationId: String, relationSide: RelationSide.Value): Boolean = {
    isRelationWithId(relationId) && this.relationSide.contains(relationSide)
  }

  private val excludedFromMutations = Vector("updatedAt", "createdAt", "id")
  def isWritable: Boolean           = !isReadonly && !excludedFromMutations.contains(name)
  def isVisible: Boolean            = !isHidden

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

  def relatedField(project: Project): Option[Field] = {
    val fields = relatedModel(project).get.fields

    val returnField = fields.find { field =>
      field.relation.exists { relation =>
        val isTheSameField    = field.id == this.id
        val isTheSameRelation = relation.id == this.relation.get.id
        isTheSameRelation && !isTheSameField
      }
    }
    val fallback = fields.find { relatedField =>
      relatedField.relation.exists { relation =>
        relation.id == this.relation.get.id
      }
    }

    returnField.orElse(fallback)
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
    fieldMirrors: List[RelationFieldMirror] = List.empty
) {
  def connectsTheModels(model1: Model, model2: Model): Boolean   = connectsTheModels(model1.id, model2.id)
  def connectsTheModels(model1: String, model2: String): Boolean = (modelAId == model1 && modelBId == model2) || (modelAId == model2 && modelBId == model1)

  def isSameModelRelation(project: Project): Boolean          = getModelA(project) == getModelB(project)
  def isSameFieldSameModelRelation(project: Project): Boolean = getModelAField(project) == getModelBField(project)

  def getModelA(project: Project): Option[Model] = project.getModelById(modelAId)
  def getModelA_!(project: Project): Model       = getModelA(project).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))

  def getModelB(project: Project): Option[Model] = project.getModelById(modelBId)
  def getModelB_!(project: Project): Model       = getModelB(project).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))

  def getOtherModel_!(project: Project, model: Model): Model = {
    model.id match {
      case `modelAId` => getModelB_!(project)
      case `modelBId` => getModelA_!(project)
      case _          => ??? //throw SystemErrors.InvalidRelation(s"The model with the id ${model.id} is not part of this relation.")
    }
  }

  def getField(project: Project, model: Model): Option[Field] = {
    model.id match {
      case `modelAId` => getModelAField(project)
      case `modelBId` => getModelBField(project)
      case _ =>
        sys.error(s"The model with the id ${model.id} is not part of this relation.") //throw SystemErrors.InvalidRelation(s"The model with the id ${model.id} is not part of this relation.")
    }
  }

  def getModelAField(project: Project): Option[Field] = modelFieldFor(project, modelAId, RelationSide.A)
  def getModelAField_!(project: Project): Field =
    getModelAField(project).get //OrElse(throw SystemErrors.InvalidRelation("A relation must have a field on model A."))

  def getModelBField(project: Project): Option[Field] = {
    // note: defaults to modelAField to handle same model, same field relations
    modelFieldFor(project, modelBId, RelationSide.B) //.orElse(getModelAField(project))
  }
  def getModelBField_!(project: Project): Field =
    getModelBField(project).get //OrElse(throw SystemErrors.InvalidRelation("This must return a Model, if not Model B then Model A."))

  private def modelFieldFor(project: Project, modelId: String, relationSide: RelationSide.Value): Option[Field] = {
    for {
      model <- project.getModelById(modelId)
      field <- model.relationFieldForIdAndSide(relationId = id, relationSide = relationSide)
    } yield field
  }

  def fieldSide(project: Project, field: Field): cool.graph.shared.models.RelationSide.Value = {
    val fieldModel = project.getModelByFieldId_!(field.id)
    fieldModel.id match {
      case `modelAId` => RelationSide.A
      case `modelBId` => RelationSide.B
    }
  }

  def getRelationFieldMirrorById(id: String): Option[RelationFieldMirror] = fieldMirrors.find(_.id == id)
  def getRelationFieldMirrorById_!(id: String): RelationFieldMirror =
    ??? //getRelationFieldMirrorById(id).getOrElse(throw SystemErrors.InvalidRelationFieldMirrorId(id))

  def sideOf(model: Model): RelationSide.Value = {
    if (model.id == modelAId) {
      RelationSide.A
    } else if (model.id == modelBId) {
      RelationSide.B
    } else {
      sys.error(s"The model ${model.name} is not part of the relation ${name}")
    }
  }

  def oppositeSideOf(model: Model): RelationSide.Value = {
    sideOf(model) match {
      case RelationSide.A => RelationSide.B
      case RelationSide.B => RelationSide.A
    }
  }
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
