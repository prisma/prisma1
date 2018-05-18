package com.prisma.shared.models

import com.prisma.gc_values.GCValue
import com.prisma.shared.errors.SharedErrors
import com.prisma.shared.models.FieldConstraintType.FieldConstraintType
import com.prisma.shared.models.Manifestations._
import org.joda.time.DateTime

object IdType {
  type Id = String
}

import com.prisma.shared.models.IdType._

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
case class ProjectWithClientId(project: Project, clientId: Id)

object Schema {
  val empty = Schema()
}

case class Schema(
    modelFns: List[Schema => Model] = List.empty,
    relationFns: List[RelationTemplate] = List.empty,
    enums: List[Enum] = List.empty
) {
  val models    = modelFns.map(_.apply(this))
  val relations = relationFns.map(_.apply(this))

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
  def getRelationByName_!(name: String): Relation       = getRelationByName(name).get

  def getRelationsThatConnectModels(modelA: String, modelB: String): List[Relation] = relations.filter(_.connectsTheModels(modelA, modelB))

}

object Model {
  def apply(
      name: String,
      stableIdentifier: String,
      fieldFns: List[Model => Field],
      manifestation: Option[ModelManifestation]
  ): Schema => Model = {
    new Model(name, stableIdentifier, fieldFns, manifestation)(_)
  }
}
class Model(
    val name: String,
    val stableIdentifier: String,
    val fieldFns: List[Model => Field],
    val manifestation: Option[ModelManifestation]
)(val schema: Schema) {
  val id: String     = name
  val dbName: String = manifestation.map(_.dbName).getOrElse(id)

  lazy val fields: List[Field]                = fieldFns.map(_.apply(this))
  lazy val uniqueFields: List[Field]          = fields.filter(f => f.isUnique && f.isVisible)
  lazy val scalarFields: List[Field]          = fields.filter(_.isScalar)
  lazy val scalarListFields: List[Field]      = scalarFields.filter(_.isList)
  lazy val scalarNonListFields: List[Field]   = scalarFields.filter(!_.isList)
  lazy val relationFields: List[Field]        = fields.filter(_.isRelation)
  lazy val relationListFields: List[Field]    = relationFields.filter(_.isList)
  lazy val relationNonListFields: List[Field] = relationFields.filter(!_.isList)
  lazy val visibleRelationFields: List[Field] = relationFields.filter(_.isVisible)
  lazy val relations: List[Relation]          = fields.flatMap(_.relation).distinct
  lazy val nonListFields                      = fields.filter(!_.isList)
  lazy val idField                            = getFieldByName("id")
  lazy val idField_!                          = getFieldByName_!("id")
  lazy val dbNameOfIdField_!                  = idField_!.dbName
  val updateAtField                           = getFieldByName("updatedAt")

  lazy val cascadingRelationFields: List[Field] = relationFields.filter(field => field.relation.get.sideOfModelCascades(this))

  def relationFieldForIdAndSide(relationId: String, relationSide: RelationSide.Value): Option[Field] = {
    fields.find(_.isRelationWithIdAndSide(relationId, relationSide))
  }

  def filterFields(fn: Field => Boolean): Model = {
    val newFields         = this.fields.filter(fn).map(_.copy())
    val newModel          = copy(fieldFns = newFields)
    val newModelsInSchema = schema.models.filter(_.name != name).map(_.copy()) :+ newModel
    schema.copy(modelFns = newModelsInSchema).getModelByName_!(name)
  }

  def getFieldById_!(id: Id): Field       = getFieldById(id).get
  def getFieldById(id: Id): Option[Field] = fields.find(_.id == id)

  def getFieldByName_!(name: String): Field =
    getFieldByName(name).getOrElse(sys.error(s"field $name is not part of the model ${this.name}")) // .getOrElse(throw FieldNotInModel(fieldName = name, modelName = this.name))
  def getFieldByName(name: String): Option[Field] = fields.find(_.name == name)

  def hasVisibleIdField: Boolean = idField.exists(_.isVisible)

  def copy(
      name: String = this.name,
      stableIdentifier: String = this.stableIdentifier,
      fieldFns: List[Model => Field] = this.fieldFns,
      manifestation: Option[ModelManifestation] = this.manifestation
  ): Schema => Model = {
    Model(name, stableIdentifier, fieldFns, manifestation)
  }
}

object RelationSide extends Enumeration {
  type RelationSide = Value
  val A = Value("A")
  val B = Value("B")

  def opposite(side: RelationSide.Value) = if (side == A) B else A
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

object Field {
  def apply(
      name: String,
      typeIdentifier: TypeIdentifier.Value,
      isRequired: Boolean,
      isList: Boolean,
      isUnique: Boolean,
      isHidden: Boolean = false,
      isReadonly: Boolean = false,
      enum: Option[Enum],
      defaultValue: Option[GCValue],
      relationName: Option[String],
      relationSide: Option[RelationSide.Value],
      manifestation: Option[FieldManifestation],
      constraints: List[FieldConstraint] = List.empty
  ): Model => Field = {
    new Field(
      name = name,
      typeIdentifier = typeIdentifier,
      isRequired = isRequired,
      isList = isList,
      isUnique = isUnique,
      isHidden = isHidden,
      isReadonly = isReadonly,
      enum = enum,
      defaultValue = defaultValue,
      relationName = relationName,
      relationSide = relationSide,
      manifestation = manifestation,
      constraints = constraints
    )(_)
  }
}
class Field(
    val name: String,
    val typeIdentifier: TypeIdentifier.Value,
    val isRequired: Boolean,
    val isList: Boolean,
    val isUnique: Boolean,
    val isHidden: Boolean,
    val isReadonly: Boolean,
    val enum: Option[Enum],
    val defaultValue: Option[GCValue],
    val relationName: Option[String],
    val relationSide: Option[RelationSide.Value],
    val manifestation: Option[FieldManifestation],
    val constraints: List[FieldConstraint]
)(model: Model) {
  val relation: Option[Relation] = relationName.flatMap(model.schema.getRelationByName)

  def id = name
  def dbName = {
    relation match {
      case Some(r) if r.isInlineRelation => r.manifestation.get.asInstanceOf[InlineRelationManifestation].referencingColumn
      case None                          => manifestation.map(_.dbName).getOrElse(name)
      case _                             => sys.error("not a valid call on relations manifested via a table")
    }
  }
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

  def copy(
      name: String = this.name,
      typeIdentifier: TypeIdentifier.Value = this.typeIdentifier,
      isRequired: Boolean = this.isRequired,
      isList: Boolean = this.isList,
      isUnique: Boolean = this.isUnique,
      isHidden: Boolean = this.isHidden,
      isReadonly: Boolean = this.isReadonly,
      enum: Option[Enum] = this.enum,
      defaultValue: Option[GCValue] = this.defaultValue,
      relationName: Option[String] = this.relationName,
      relationSide: Option[RelationSide.Value] = this.relationSide,
      manifestation: Option[FieldManifestation] = this.manifestation,
      constraints: List[FieldConstraint] = this.constraints
  ): Model => Field = {
    Field(
      name = name,
      typeIdentifier = typeIdentifier,
      isRequired = isRequired,
      isList = isList,
      isUnique = isUnique,
      isHidden = isHidden,
      isReadonly = isReadonly,
      enum = enum,
      defaultValue = defaultValue,
      relationName = relationName,
      relationSide = relationSide,
      manifestation = manifestation,
      constraints = constraints
    )
  }
}

sealed trait FieldConstraint {
  val id: String; val fieldId: String; val constraintType: FieldConstraintType
}

case class StringConstraint(
    id: String,
    fieldId: String,
    equalsString: Option[String] = None,
    oneOfString: List[String] = List.empty,
    minLength: Option[Int] = None,
    maxLength: Option[Int] = None,
    startsWith: Option[String] = None,
    endsWith: Option[String] = None,
    includes: Option[String] = None,
    regex: Option[String] = None
) extends FieldConstraint {
  val constraintType: FieldConstraintType = FieldConstraintType.STRING
}

case class NumberConstraint(
    id: String,
    fieldId: String,
    equalsNumber: Option[Double] = None,
    oneOfNumber: List[Double] = List.empty,
    min: Option[Double] = None,
    max: Option[Double] = None,
    exclusiveMin: Option[Double] = None,
    exclusiveMax: Option[Double] = None,
    multipleOf: Option[Double] = None
) extends FieldConstraint {
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

case class RelationTemplate(
    name: String,
    // BEWARE: if the relation looks like this: val relation = Relation(id = "relationId", modelAId = "userId", modelBId = "todoId")
    // then the relationSide for the fields have to be "opposite", because the field's side is the side of _the other_ model
    // val userField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.B)
    // val todoField = Field(..., relation = Some(relation), relationSide = Some(RelationSide.A)
    modelAId: Id,
    modelBId: Id,
    modelAOnDelete: OnDelete.Value,
    modelBOnDelete: OnDelete.Value,
    manifestation: Option[RelationManifestation]
) extends (Schema => Relation) {
  def apply(schema: Schema) = new Relation(this, schema)

  def connectsTheModels(model1: String, model2: String): Boolean = (modelAId == model1 && modelBId == model2) || (modelAId == model2 && modelBId == model1)

  def isSameModelRelation: Boolean = modelAId == modelBId
}

class Relation(
    val template: RelationTemplate,
    val schema: Schema
) {
  val name                                                       = template.name
  val modelAId                                                   = template.modelAId
  val modelBId                                                   = template.modelBId
  val modelAOnDelete                                             = template.modelAOnDelete
  val modelBOnDelete                                             = template.modelBOnDelete
  val manifestation                                              = template.manifestation
  val isSameModelRelation: Boolean                               = template.isSameModelRelation
  def connectsTheModels(model1: String, model2: String): Boolean = template.connectsTheModels(model1, model2)

  val relationTableName = manifestation.collect { case m: RelationTableManifestation => m.table }.getOrElse("_" + name)

  def relationTableNameNew(schema: Schema): String =
    manifestation
      .collect {
        case m: RelationTableManifestation  => m.table
        case m: InlineRelationManifestation => schema.getModelById_!(m.inTableOfModelId).dbName
      }
      .getOrElse("_" + name)

  def modelAColumn(schema: Schema): String = manifestation match {
    case Some(m: RelationTableManifestation) =>
      m.modelAColumn
    case Some(m: InlineRelationManifestation) =>
      if (m.inTableOfModelId == modelAId) getModelA_!(schema).idField_!.dbName else m.referencingColumn
    case None =>
      "A"
  }

  def modelBColumn(schema: Schema): String = manifestation match {
    case Some(m: RelationTableManifestation) =>
      m.modelBColumn
    case Some(m: InlineRelationManifestation) =>
      if (m.inTableOfModelId == modelBId && !isSameModelRelation) getModelB_!(schema).idField_!.dbName else m.referencingColumn
    case None =>
      "B"
  }

  def columnForRelationSide(schema: Schema, relationSide: RelationSide.Value): String = {
    if (relationSide == RelationSide.A) modelAColumn(schema) else modelBColumn(schema)
  }

  def hasManifestation: Boolean = manifestation.isDefined
  def isInlineRelation: Boolean = manifestation.exists(_.isInstanceOf[InlineRelationManifestation])

  def inlineManifestation: Option[InlineRelationManifestation] = manifestation.collect { case x: InlineRelationManifestation => x }

  def isSameFieldSameModelRelation(schema: Schema): Boolean = {
    // note: defaults to modelAField to handle same model, same field relations
    getModelAField(schema) == getModelBField(schema).orElse(getModelAField(schema))
  }

  def isManyToMany(schema: Schema): Boolean = {
    val modelAFieldIsList = getModelAField(schema).map(_.isList).getOrElse(true)
    val modelBFieldIsList = getModelBField(schema).map(_.isList).getOrElse(true)
    modelAFieldIsList && modelBFieldIsList
  }

  def getFieldOnModel(modelId: String, schema: Schema): Option[Field] = {
    if (modelId == modelAId) {
      getModelAField(schema)
    } else if (modelId == modelBId) {
      getModelBField(schema)
    } else {
      sys.error(s"The model id ${modelId} is not part of this relation ${name}")
    }
  }

  def getModelA(schema: Schema): Option[Model] = schema.getModelById(modelAId)
  def getModelA_!(schema: Schema): Model       = getModelA(schema).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model A."))

  def getModelB(schema: Schema): Option[Model] = schema.getModelById(modelBId)
  def getModelB_!(schema: Schema): Model       = getModelB(schema).get //OrElse(throw SystemErrors.InvalidRelation("A relation should have a valid Model B."))

  def getModelAField(schema: Schema): Option[Field] = modelFieldFor(schema, modelAId, RelationSide.A)
  def getModelBField(schema: Schema): Option[Field] = modelFieldFor(schema, modelBId, RelationSide.B)

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
