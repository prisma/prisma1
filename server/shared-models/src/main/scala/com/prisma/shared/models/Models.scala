package com.prisma.shared.models

import com.prisma.gc_values.GCValue
import com.prisma.shared.errors.SharedErrors
import com.prisma.shared.models.FieldConstraintType.FieldConstraintType
import org.joda.time.DateTime

object IdType {
  type Id = String
}

import com.prisma.shared.models.IdType._

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

sealed trait Function {
  def name: String
  def isActive: Boolean
  def delivery: FunctionDelivery
  def typeCode: FunctionType.Value
}

object FunctionType extends Enumeration {
  val ServerSideSubscription = Value("server-side-subscription")
}

case class ServerSideSubscriptionFunction(
    name: String,
    isActive: Boolean,
    delivery: FunctionDelivery,
    query: String
) extends Function {
  override def typeCode = FunctionType.ServerSideSubscription
}

sealed trait FunctionDelivery {
  def typeCode: FunctionDeliveryType.Value
}

object FunctionDeliveryType extends Enumeration {
  val WebhookDelivery = Value("webhook-delivery")
}

case class WebhookDelivery(
    url: String,
    headers: Vector[(String, String)]
) extends FunctionDelivery {
  override def typeCode = FunctionDeliveryType.WebhookDelivery
}

case class Schema(
    models: List[Model] = List.empty,
    relations: List[Relation] = List.empty,
    enums: List[Enum] = List.empty
) {
  def allFields: Seq[Field] = models.flatMap(_.fields)

  def hasSchemaNameConflict(name: String, id: String): Boolean = {
    val conflictingType = this.models.exists(model => List(s"create${model.name}", s"update${model.name}", s"delete${model.name}").contains(name))
    conflictingType
  }

  def getModelById(id: Id): Option[Model] = models.find(_.id == id)
  def getModelById_!(id: Id): Model       = getModelById(id).getOrElse(throw SharedErrors.InvalidModel(id))

  def getModelByStableIdentifier_!(stableId: String): Model = {
    models.find(_.stableIdentifier == stableId).getOrElse(throw SharedErrors.InvalidModel(s"Could not find a model for the stable identifier: $stableId"))
  }

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

  // note: mysql columns are case insensitive, so we have to be as well
  def getEnumByName(name: String): Option[Enum] = enums.find(_.name.toLowerCase == name.toLowerCase)

  def getRelationById(id: Id): Option[Relation] = relations.find(_.id == id)
  def getRelationById_!(id: Id): Relation       = getRelationById(id).get //OrElse(throw SystemErrors.InvalidRelationId(id))

  def getRelationByName(name: String): Option[Relation] = relations.find(_.name == name)
  def getRelationByName_!(name: String): Relation =
    getRelationByName(name).get //OrElse(throw SystemErrors.InvalidRelation("There is no relation with name: " + name))

  def getFieldsByRelationId(id: Id): List[Field] = models.flatMap(_.fields).filter(f => f.relation.isDefined && f.relation.get.id == id)

  def getUnambiguousRelationThatConnectsModels_!(modelA: String, modelB: String): Option[Relation] = {
    val candidates = relations.filter(_.connectsTheModels(modelA, modelB))
    require(candidates.size < 2, "This method must only be called for unambiguous relations!")
    candidates.headOption
  }

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
}

case class Project(
    id: Id,
    ownerId: Id,
    revision: Int = 1,
    schema: Schema,
    webhookUrl: Option[String] = None,
    secrets: Vector[String] = Vector.empty,
    allowQueries: Boolean = true,
    allowMutations: Boolean = true,
    functions: List[Function] = List.empty
) {
  def models    = schema.models
  def relations = schema.relations
  def enums     = schema.enums

  lazy val projectId: ProjectId       = ProjectId.fromEncodedString(id)
  val serverSideSubscriptionFunctions = functions.collect { case x: ServerSideSubscriptionFunction => x }

  def getFunctionByName(name: String): Option[Function] = functions.find(_.name == name)
  def getFunctionByName_!(name: String): Function       = getFunctionByName(name).get //OrElse(throw SystemErrors.InvalidFunctionName(name))
}
object ProjectWithClientId {
  def apply(project: Project): ProjectWithClientId = ProjectWithClientId(project, project.ownerId)
}
case class ProjectWithClientId(project: Project, clientId: Id) {
  val id: Id = project.id
}
case class ProjectWithClient(project: Project, client: Client)

case class Model(
    name: String,
    stableIdentifier: String,
    fields: List[Field],
    description: Option[String] = None,
) {
  def id = name

  lazy val uniqueFields: List[Field]          = fields.filter(f => f.isUnique && f.isVisible)
  lazy val scalarFields: List[Field]          = fields.filter(_.isScalar)
  lazy val scalarListFields: List[Field]      = scalarFields.filter(_.isList)
  lazy val scalarNonListFields: List[Field]   = scalarFields.filter(!_.isList)
  lazy val relationFields: List[Field]        = fields.filter(_.isRelation)
  lazy val relationListFields: List[Field]    = relationFields.filter(_.isList)
  lazy val relationNonListFields: List[Field] = relationFields.filter(!_.isList)
  lazy val relations: List[Relation]          = fields.flatMap(_.relation).distinct

  lazy val cascadingRelationFields: List[Field] = relationFields.filter(field => field.relation.get.sideOfModelCascades(this))

  def relationFieldForIdAndSide(relationId: String, relationSide: RelationSide.Value): Option[Field] = {
    fields.find(_.isRelationWithIdAndSide(relationId, relationSide))
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
    name: String,
    values: Vector[String] = Vector.empty
)

case class Field(
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
  def id                                            = name
  def isScalar: Boolean                             = typeIdentifier != TypeIdentifier.Relation
  def isRelation: Boolean                           = typeIdentifier == TypeIdentifier.Relation
  def isRelationWithId(relationId: String): Boolean = relation.exists(_.id == relationId)

  def isRelationWithIdAndSide(relationId: String, relationSide: RelationSide.Value): Boolean = {
    isRelationWithId(relationId) && this.relationSide.contains(relationSide)
  }

  def isOneToOneRelation(project: Project): Boolean = {
    val otherField = this.relatedField(project.schema).get
    !this.isList && !otherField.isList
  }

  def isManyToManyRelation(project: Project): Boolean = {
    val otherField = this.relatedField(project.schema).get
    this.isList && otherField.isList
  }

  def isOneToManyRelation(project: Project): Boolean = {
    val otherField = this.relatedField(project.schema).get
    (this.isList && !otherField.isList) || (!this.isList && otherField.isList)
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

  def relatedModel_!(schema: Schema): Model = {
    relatedModel(schema) match {
      case None        => sys.error(s"Could not find relatedModel for field [$name] on model [${model(schema)}]")
      case Some(model) => model
    }
  }

  def relatedModel(schema: Schema): Option[Model] = {
    relation.flatMap(relation => {
      relationSide match {
        case Some(RelationSide.A) => relation.getModelB(schema)
        case Some(RelationSide.B) => relation.getModelA(schema)
        case x                    => ??? //throw SystemErrors.InvalidStateException(message = s" relationSide was $x")
      }
    })

  }

  def model(schema: Schema): Option[Model] = {
    relation.flatMap(relation => {
      relationSide match {
        case Some(RelationSide.A) => relation.getModelA(schema)
        case Some(RelationSide.B) => relation.getModelB(schema)
        case x                    => ??? //throw SystemErrors.InvalidStateException(message = s" relationSide was $x")
      }
    })
  }

  //todo this is dangerous in combination with self relations since it will return the field itself as related field
  def relatedField(schema: Schema): Option[Field] = {
    val fields = relatedModel(schema).get.fields

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

  //this really does return None if there is no opposite field
  def otherRelationField(schema: Schema): Option[Field] = {
    val fields = relatedModel(schema).get.fields

    fields.find { field =>
      field.relation.exists { relation =>
        val isTheSameField    = field.id == this.id
        val isTheSameRelation = relation.id == this.relation.get.id
        isTheSameRelation && !isTheSameField
      }
    }
  }

  def otherSideIsRequired(project: Project): Boolean = relatedField(project.schema) match {
    case Some(f) if f.isRequired => true
    case Some(_)                 => false
    case None                    => false
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
    name: String,
    description: Option[String] = None,
    // BEWARE: if the relation looks like this: val relation = Relation(id = "relationId", modelAId = "userId", modelBId = "todoId")
    // then the relationSide for the fields have to be "opposite", because the field's side is the side of _the other_ model
    // val userField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.B)
    // val todoField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.A)
    modelAId: Id,
    modelBId: Id,
    modelAOnDelete: OnDelete.Value,
    modelBOnDelete: OnDelete.Value
) {
  val id = "_" + name

  def connectsTheModels(model1: Model, model2: Model): Boolean   = connectsTheModels(model1.id, model2.id)
  def connectsTheModels(model1: String, model2: String): Boolean = (modelAId == model1 && modelBId == model2) || (modelAId == model2 && modelBId == model1)

  def isUnambiguous(schema: Schema): Boolean = (schema.relations.toSet - this).nonEmpty

  def isSameModelRelation(schema: Schema): Boolean          = getModelA(schema) == getModelB(schema)
  def isSameFieldSameModelRelation(schema: Schema): Boolean = getModelAField(schema) == getModelBField(schema)

  def getModelA(schema: Schema): Option[Model] = schema.getModelById(modelAId)
  def getModelA_!(schema: Schema): Model       = getModelA(schema).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))

  def getModelB(schema: Schema): Option[Model] = schema.getModelById(modelBId)
  def getModelB_!(schema: Schema): Model       = getModelB(schema).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))

  def getOtherModel_!(schema: Schema, model: Model): Model = {
    model.id match {
      case `modelAId` => getModelB_!(schema)
      case `modelBId` => getModelA_!(schema)
      case _          => ??? //throw SystemErrors.InvalidRelation(s"The model with the id ${model.id} is not part of this relation.")
    }
  }

  def getField(schema: Schema, model: Model): Option[Field] = {
    model.id match {
      case `modelAId` => getModelAField(schema)
      case `modelBId` => getModelBField(schema)
      case _ =>
        sys.error(s"The model with the id ${model.id} is not part of this relation.") //throw SystemErrors.InvalidRelation(s"The model with the id ${model.id} is not part of this relation.")
    }
  }

  def getModelAField(schema: Schema): Option[Field] = modelFieldFor(schema, modelAId, RelationSide.A)
  def getModelBField(schema: Schema): Option[Field] = {
    // note: defaults to modelAField to handle same model, same field relations
    modelFieldFor(schema, modelBId, RelationSide.B) //.orElse(getModelAField(project))
  }

  private def modelFieldFor(schema: Schema, modelId: String, relationSide: RelationSide.Value): Option[Field] = {
    for {
      model <- schema.getModelById(modelId)
      field <- model.relationFieldForIdAndSide(relationId = id, relationSide = relationSide)
    } yield field
  }

  def fieldSide(schema: Schema, field: Field): com.prisma.shared.models.RelationSide.Value = {
    val fieldModel = schema.getModelByFieldId_!(field.id)
    fieldModel.id match {
      case `modelAId` => RelationSide.A
      case `modelBId` => RelationSide.B
    }
  }

  def sideOf(model: Model): RelationSide.Value = {
    if (model.id == modelAId) {
      RelationSide.A
    } else if (model.id == modelBId) {
      RelationSide.B
    } else {
      sys.error(s"The model ${model.name} is not part of the relation $name")
    }
  }

  def sideOfModelCascades(model: Model): Boolean = {
    if (model.id == modelAId) {
      modelAOnDelete == OnDelete.Cascade
    } else if (model.id == modelBId) {
      modelBOnDelete == OnDelete.Cascade
    } else {
      sys.error(s"The model ${model.name} is not part of the relation $name")
    }
  }

  def bothSidesCascade: Boolean = modelAOnDelete == OnDelete.Cascade && modelBOnDelete == OnDelete.Cascade

  def oppositeSideOf(model: Model): RelationSide.Value = {
    sideOf(model) match {
      case RelationSide.A => RelationSide.B
      case RelationSide.B => RelationSide.A
    }
  }
}

object ModelMutationType extends Enumeration {
  type ModelMutationType = Value
  val Created = Value("CREATED")
  val Updated = Value("UPDATED")
  val Deleted = Value("DELETED")
}
