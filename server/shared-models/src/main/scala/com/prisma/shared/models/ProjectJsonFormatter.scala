package com.prisma.shared.models

import com.prisma.gc_values._
import com.prisma.shared.models.FieldConstraintType.FieldConstraintType
import com.prisma.shared.models.Manifestations._
import com.prisma.shared.models.MigrationStepsJsonFormatter._
import com.prisma.utils.json.JsonUtils
import com.prisma.utils.json.JsonUtils._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ProjectJsonFormatter {

  // ENUMS
  implicit lazy val relationSide        = enumFormat(RelationSide)
  implicit lazy val typeIdentifier      = enumFormat(TypeIdentifier)
  implicit lazy val fieldConstraintType = enumFormat(FieldConstraintType)
  implicit lazy val modelMutationType   = enumFormat(ModelMutationType)

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

  implicit lazy val gcValueFormat = new Format[GCValue] {
    val discriminatorField = "gcValueType"
    val isListField        = "isList"
    val valueField         = "value"
    val nullType           = "null"
    val stringType         = "string"
    val passwordType       = "password"
    val enumType           = "enum"
    val graphQlIdType      = "graphQlId"
    val dateTimeType       = "datetime"
    val intType            = "int"
    val floatType          = "float"
    val booleanType        = "bool"
    val jsonType           = "json"
    val listType           = "list"
    val rootType           = "root"

    override def reads(json: JsValue): JsResult[GCValue] = {
      for {
        discriminator <- (json \ discriminatorField).validate[String]
        value         <- (json \ valueField).validate[JsValue]
        isList        <- (json \ isListField).validate[Boolean]
        converted     <- createGcValue(discriminator, value, isList)
      } yield converted
    }

    private def createGcValue(discriminator: String, value: JsValue, isList: Boolean): JsResult[GCValue] = (discriminator, value) match {
      case (`nullType`, _)                  => JsSuccess(NullGCValue)
      case (`stringType`, JsString(str))    => JsSuccess(StringGCValue(str))
      case (`enumType`, JsString(str))      => JsSuccess(EnumGCValue(str))
      case (`graphQlIdType`, JsString(str)) => JsSuccess(IdGCValue(str))
      case (`dateTimeType`, JsString(str))  => JsSuccess(DateTimeGCValue(new DateTime(str, DateTimeZone.UTC)))
      case (`intType`, JsNumber(x))         => JsSuccess(IntGCValue(x.toInt))
      case (`floatType`, JsNumber(x))       => JsSuccess(FloatGCValue(x.toDouble))
      case (`booleanType`, JsBoolean(x))    => JsSuccess(BooleanGCValue(x))
      case (`jsonType`, json)               => JsSuccess(JsonGCValue(json))
      case (_, JsArray(elements)) if isList =>
        val gcValues = elements.map(element => this.reads(element))
        gcValues.find(_.isError) match {
          case Some(error) => error
          case None        => JsSuccess(ListGCValue(gcValues.map(_.get).toVector))
        }
      case _ => JsError(s"invalid discriminator and value combination: $discriminator and value $value")
    }

    override def writes(gcValue: GCValue): JsValue = {
      val formatter = ISODateTimeFormat.dateHourMinuteSecondFraction()

      gcValue match {
        case NullGCValue        => json(nullType, JsNull)
        case x: StringGCValue   => json(stringType, JsString(x.value))
        case x: EnumGCValue     => json(enumType, JsString(x.value))
        case x: IdGCValue       => json(graphQlIdType, JsString(x.value))
        case x: DateTimeGCValue => json(dateTimeType, JsString(formatter.print(x.value)))
        case x: IntGCValue      => json(intType, JsNumber(x.value))
        case x: FloatGCValue    => json(floatType, JsNumber(x.value))
        case x: BooleanGCValue  => json(booleanType, JsBoolean(x.value))
        case x: JsonGCValue     => json(jsonType, x.value)
        case x: ListGCValue     => json(listType, JsArray(x.values.map(this.writes)), isList = true)
        case x: RootGCValue     => json(rootType, JsObject(x.map.mapValues(this.writes)))
      }
    }

    private def json(discriminator: String, valueAsJson: JsValue, isList: Boolean = false): JsObject = {
      Json.obj(
        discriminatorField -> discriminator,
        isListField        -> isList,
        valueField         -> valueAsJson
      )
    }
  }

  implicit lazy val webhookDelivery = Json.format[WebhookDelivery]
  implicit lazy val functionDelivery = new OFormat[FunctionDelivery] {
    val discriminatorField = "type"

    override def reads(json: JsValue) = {
      (json \ discriminatorField).validate[String].map(FunctionDeliveryType.withName).flatMap {
        case FunctionDeliveryType.WebhookDelivery => webhookDelivery.reads(json)
      }
    }

    override def writes(delivery: FunctionDelivery) = {
      val objectJson = delivery match {
        case x: WebhookDelivery => webhookDelivery.writes(x)
      }
      addDiscriminator(objectJson, delivery)
    }

    private def addDiscriminator(json: JsObject, delivery: FunctionDelivery) = json ++ Json.obj(discriminatorField -> delivery.typeCode.toString)
  }

  implicit lazy val sssFn = Json.format[ServerSideSubscriptionFunction]
  implicit lazy val function = new OFormat[Function] {
    val discriminatorField = "type"

    override def reads(json: JsValue): JsResult[ServerSideSubscriptionFunction] = {
      (json \ discriminatorField).validate[String].map(FunctionType.withName).flatMap {
        case FunctionType.ServerSideSubscription => sssFn.reads(json)
      }
    }

    override def writes(fn: Function): JsObject = {
      val objectJson = fn match {
        case x: ServerSideSubscriptionFunction => sssFn.writes(x)
      }
      addDiscriminator(objectJson, fn)
    }

    private def addDiscriminator(json: JsObject, fn: Function) = json ++ Json.obj(discriminatorField -> fn.typeCode.toString)
  }

  val inlineRelationManifestationFormat: OFormat[InlineRelationManifestation] = (
    (JsPath \ "inTableOfModelId").format[String] and
      (JsPath \ "referencingColumn").format[String]
  )(InlineRelationManifestation.apply, unlift(InlineRelationManifestation.unapply))

  val relationTableManifestationFormat: OFormat[RelationTableManifestation] = (
    (JsPath \ "table").format[String] and
      (JsPath \ "modelAColumn").format[String] and
      (JsPath \ "modelBColumn").format[String]
  )(RelationTableManifestation.apply, unlift(RelationTableManifestation.unapply))

  implicit lazy val relationManifestation = new Format[RelationManifestation] {
    val discriminatorField = "relationManifestationType"
    val inlineRelationType = "inline"
    val relationTableType  = "relation_table"

    override def reads(json: JsValue) = (json \ discriminatorField).validate[String].flatMap {
      case `inlineRelationType` => inlineRelationManifestationFormat.reads(json)
      case `relationTableType`  => relationTableManifestationFormat.reads(json)
    }

    override def writes(mani: RelationManifestation) = {
      mani match {
        case x: InlineRelationManifestation => inlineRelationManifestationFormat.writes(x) ++ Json.obj(discriminatorField -> inlineRelationType)
        case x: RelationTableManifestation  => relationTableManifestationFormat.writes(x) ++ Json.obj(discriminatorField  -> relationTableType)
      }
    }
  }

  implicit val relationWrites: Writes[RelationTemplate] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "modelAId").write[String] and
      (JsPath \ "modelBId").write[String] and
      (JsPath \ "modelAOnDelete").write[OnDelete.Value] and
      (JsPath \ "modelBOnDelete").write[OnDelete.Value] and
      (JsPath \ "manifestation").writeNullable[RelationManifestation]
  )(unlift(RelationTemplate.unapply))

  implicit val relationReads: Reads[RelationTemplate] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "modelAId").read[String] and
      (JsPath \ "modelBId").read[String] and
      (JsPath \ "modelAOnDelete").readNullable[OnDelete.Value].map(_.getOrElse(OnDelete.SetNull)) and
      (JsPath \ "modelBOnDelete").readNullable[OnDelete.Value].map(_.getOrElse(OnDelete.SetNull)) and
      (JsPath \ "manifestation").readNullable[RelationManifestation]
  )(RelationTemplate.apply _)

  implicit val modelManifestationWrites: Writes[ModelManifestation] = Writes(manifestation => Json.obj("dbName" -> manifestation.dbName))
  implicit val modelManifestationReads: Reads[ModelManifestation]   = (JsPath \ "dbName").read[String].map(ModelManifestation)
  implicit val fieldManifestationWrites: Writes[FieldManifestation] = Writes(manifestation => Json.obj("dbName" -> manifestation.dbName))
  implicit val fieldManifestationReads: Reads[FieldManifestation]   = (JsPath \ "dbName").read[String].map(FieldManifestation)
  implicit lazy val enum                                            = Json.format[Enum]

  implicit val fieldReads: Reads[FieldTemplate] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "typeIdentifier").read[TypeIdentifier.Value] and
      (JsPath \ "isRequired").read[Boolean] and
      (JsPath \ "isList").read[Boolean] and
      (JsPath \ "isUnique").read[Boolean] and
      (JsPath \ "isHidden").read[Boolean] and
      (JsPath \ "isReadonly").read[Boolean] and
      (JsPath \ "enum").readNullable[Enum] and
      (JsPath \ "defaultValue").readNullable[GCValue] and
      readEitherPathNullable[String](JsPath \ "relation" \ "name", JsPath \ "relationName") and
      (JsPath \ "relationSide").readNullable[RelationSide.Value] and
      (JsPath \ "manifestation").readNullable[FieldManifestation] and
      (JsPath \ "constraints").read[List[FieldConstraint]]
  )(FieldTemplate.apply _)

  implicit val fieldWrites: Writes[FieldTemplate] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "typeIdentifier").write[TypeIdentifier.Value] and
      (JsPath \ "isRequired").write[Boolean] and
      (JsPath \ "isList").write[Boolean] and
      (JsPath \ "isUnique").write[Boolean] and
      (JsPath \ "isHidden").write[Boolean] and
      (JsPath \ "isReadonly").write[Boolean] and
      (JsPath \ "enum").writeNullable[Enum] and
      (JsPath \ "defaultValue").writeNullable[GCValue] and
      (JsPath \ "relationName").writeNullable[String] and
      (JsPath \ "relationSide").writeNullable[RelationSide.Value] and
      (JsPath \ "manifestation").writeNullable[FieldManifestation] and
      (JsPath \ "constraints").write[List[FieldConstraint]]
  )(unlift(FieldTemplate.unapply))

  implicit val modelReads: Reads[ModelTemplate] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "stableIdentifier").read[String] and
      (JsPath \ "fields").read[List[FieldTemplate]] and
      (JsPath \ "manifestation").readNullable[ModelManifestation]
  )(ModelTemplate.apply _)

  implicit val modelWrites: Writes[ModelTemplate] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "stableIdentifier").write[String] and
      (JsPath \ "fields").write[List[FieldTemplate]] and
      (JsPath \ "manifestation").writeNullable[ModelManifestation]
  )(unlift(ModelTemplate.unapply))

  val schemaReads: Reads[Schema] = (
    (JsPath \ "models").read[List[ModelTemplate]] and
      (JsPath \ "relations").read[List[RelationTemplate]] and
      (JsPath \ "enums").read[List[Enum]]
  )(Schema.apply _)

  val schemaWrites: Writes[Schema] = (
    (JsPath \ "models").write[List[ModelTemplate]] and
      (JsPath \ "relations").write[List[RelationTemplate]] and
      (JsPath \ "enums").write[List[Enum]]
  )(s => (s.modelTemplates, s.relationTemplates, s.enums))

  implicit lazy val schemaFormat              = Format(schemaReads, schemaWrites)
  implicit lazy val projectFormat             = Json.format[Project]
  implicit lazy val projectWithClientIdFormat = Json.format[ProjectWithClientId]
  implicit lazy val migrationStatusFormat     = JsonUtils.enumFormat(MigrationStatus)
  implicit lazy val migrationStepsFormat      = Json.format[Migration]

  def failingFormat[T] = new Format[T] {

    override def reads(json: JsValue) = fail
    override def writes(o: T)         = fail

    def fail = sys.error("This JSON Formatter always fails.")
  }

  def readEitherPathNullable[T](path1: JsPath, path2: JsPath)(implicit reads: Reads[T]): Reads[Option[T]] = {
    Reads { json =>
      val path1Lookup = path1.asSingleJson(json)
      val path2Lookup = path2.asSingleJson(json)
      (path1Lookup, path2Lookup) match {
        case (JsDefined(foundJson), _) => foundJson.validateOpt[T]
        case (_, JsDefined(foundJson)) => foundJson.validateOpt[T]
        case _                         => JsSuccess(None)
      }
    }
  }
}
