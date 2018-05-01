package com.prisma.shared.models

import com.prisma.gc_values.GCValue
import com.prisma.shared.errors.SharedErrors
import com.prisma.shared.models.FieldConstraintType.FieldConstraintType
import com.prisma.shared.models.Manifestations.{FieldManifestation, ModelManifestation}
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

  def fieldsWhereThisModelIsRequired(model: Model) = allFields.filter(f => f.isRequired && !f.isList && f.relatedModel(this).contains(model))

  def getModelById(id: Id): Option[Model] = models.find(_.id == id)
  def getModelById_!(id: Id): Model       = getModelById(id).getOrElse(throw SharedErrors.InvalidModel(id))

  def getModelByStableIdentifier_!(stableId: String): Model = {
    models.find(_.stableIdentifier == stableId).getOrElse(throw SharedErrors.InvalidModel(s"Could not find a model for the stable identifier: $stableId"))
  }

  // note: mysql columns are case insensitive, so we have to be as well. But we could make them case sensitive https://dev.mysql.com/doc/refman/5.6/en/case-sensitivity.html
  def getModelByName(name: String): Option[Model] = models.find(_.name.toLowerCase() == name.toLowerCase())
  def getModelByName_!(name: String): Model       = getModelByName(name).getOrElse(throw SharedErrors.InvalidModel(s"No model with name: $name found."))

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

  def getRelationByName(name: String): Option[Relation] = relations.find(_.name == name)
  def getRelationByName_!(name: String): Relation =
    getRelationByName(name).get //OrElse(throw SystemErrors.InvalidRelation("There is no relation with name: " + name))

  def getRelationsThatConnectModels(modelA: String, modelB: String): List[Relation] = relations.filter(_.connectsTheModels(modelA, modelB))

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

  val serverSideSubscriptionFunctions = functions.collect { case x: ServerSideSubscriptionFunction => x }

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
    manifestation: Option[ModelManifestation]
) {
  val id: String     = name
  val dbName: String = manifestation.map(_.dbName).getOrElse(id)

  lazy val uniqueFields: List[Field]          = fields.filter(f => f.isUnique && f.isVisible)
  lazy val scalarFields: List[Field]          = fields.filter(_.isScalar)
  lazy val scalarListFields: List[Field]      = scalarFields.filter(_.isList)
  lazy val scalarNonListFields: List[Field]   = scalarFields.filter(!_.isList)
  lazy val relationFields: List[Field]        = fields.filter(_.isRelation)
  lazy val relationListFields: List[Field]    = relationFields.filter(_.isList)
  lazy val relationNonListFields: List[Field] = relationFields.filter(!_.isList)
  lazy val relations: List[Relation]          = fields.flatMap(_.relation).distinct
  lazy val nonListFields                      = fields.filter(!_.isList)

  lazy val cascadingRelationFields: List[Field] = relationFields.filter(field => field.relation.get.sideOfModelCascades(this))

  def relationFieldForIdAndSide(relationId: String, relationSide: RelationSide.Value): Option[Field] = {
    fields.find(_.isRelationWithIdAndSide(relationId, relationSide))
  }

  def filterFields(fn: Field => Boolean): Model = copy(fields = this.fields.filter(fn))

  def getFieldById_!(id: Id): Field       = getFieldById(id).get
  def getFieldById(id: Id): Option[Field] = fields.find(_.id == id)

  def getFieldByName_!(name: String): Field =
    getFieldByName(name).getOrElse(sys.error(s"field $name is not part of the model ${this.name}")) // .getOrElse(throw FieldNotInModel(fieldName = name, modelName = this.name))
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
    manifestation: Option[FieldManifestation],
    constraints: List[FieldConstraint] = List.empty
) {
  def id                                            = name
  val dbName                                        = manifestation.map(_.dbName).getOrElse(name)
  def isScalar: Boolean                             = typeIdentifier != TypeIdentifier.Relation
  def isRelation: Boolean                           = typeIdentifier == TypeIdentifier.Relation
  def isScalarList: Boolean                         = isScalar && isList
  def isScalarNonList: Boolean                      = isScalar && !isList
  def isRelationList: Boolean                       = isRelation && isList
  def isRelationNonList: Boolean                    = isRelation && !isList
  def isRelationWithId(relationId: String): Boolean = relation.exists(_.relationTableName == relationId)

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
  //this should be removed where possible
  def relatedField(schema: Schema): Option[Field] = {
    val fields = relatedModel(schema).get.fields

    val returnField = fields.find { field =>
      field.relation.exists { relation =>
        val isTheSameField    = field.id == this.id
        val isTheSameRelation = relation.relationTableName == this.relation.get.relationTableName
        isTheSameRelation && !isTheSameField
      }
    }
    val fallback = fields.find { relatedField =>
      relatedField.relation.exists { relation =>
        relation.relationTableName == this.relation.get.relationTableName
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
        val isTheSameRelation = relation.relationTableName == this.relation.get.relationTableName
        isTheSameRelation && !isTheSameField
      }
    }
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
  val relationTableName = "_" + name

  def connectsTheModels(model1: String, model2: String): Boolean = (modelAId == model1 && modelBId == model2) || (modelAId == model2 && modelBId == model1)

  def isSameModelRelation: Boolean = modelAId == modelBId
  def isSameFieldSameModelRelation(schema: Schema): Boolean = {
    // note: defaults to modelAField to handle same model, same field relations
    getModelAField(schema) == getModelBField(schema).orElse(getModelAField(schema))
  }

  def getModelA(schema: Schema): Option[Model] = schema.getModelById(modelAId)
  def getModelA_!(schema: Schema): Model       = getModelA(schema).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))

  def getModelB(schema: Schema): Option[Model] = schema.getModelById(modelBId)
  def getModelB_!(schema: Schema): Model       = getModelB(schema).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))

  def getModelAField(schema: Schema): Option[Field] = modelFieldFor(schema, modelAId, RelationSide.A)
  def getModelBField(schema: Schema): Option[Field] = {
    modelFieldFor(schema, modelBId, RelationSide.B)
  }

  private def modelFieldFor(schema: Schema, modelId: String, relationSide: RelationSide.Value): Option[Field] = {
    for {
      model <- schema.getModelById(modelId)
      field <- model.relationFieldForIdAndSide(relationId = relationTableName, relationSide = relationSide)
    } yield field
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
}

object ModelMutationType extends Enumeration {
  type ModelMutationType = Value
  val Created = Value("CREATED")
  val Updated = Value("UPDATED")
  val Deleted = Value("DELETED")
}
