package com.prisma.shared.models

import com.prisma.gc_values._
import com.prisma.shared.models.FieldConstraintType.FieldConstraintType
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
      case (`graphQlIdType`, JsString(str)) => JsSuccess(GraphQLIdGCValue(str))
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
        case NullGCValue         => json(nullType, JsNull)
        case x: StringGCValue    => json(stringType, JsString(x.value))
        case x: EnumGCValue      => json(enumType, JsString(x.value))
        case x: GraphQLIdGCValue => json(graphQlIdType, JsString(x.value))
        case x: DateTimeGCValue  => json(dateTimeType, JsString(formatter.print(x.value)))
        case x: IntGCValue       => json(intType, JsNumber(x.value))
        case x: FloatGCValue     => json(floatType, JsNumber(x.value))
        case x: BooleanGCValue   => json(booleanType, JsBoolean(x.value))
        case x: JsonGCValue      => json(jsonType, x.value)
        case x: ListGCValue      => json(listType, JsArray(x.values.map(this.writes)), isList = true)
        case x: RootGCValue      => json(rootType, JsObject(x.map.mapValues(this.writes)))
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

  val relationWrites: Writes[Relation] = (
    (JsPath \ "name").write[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "modelAId").write[String] and
      (JsPath \ "modelBId").write[String] and
      (JsPath \ "modelAOnDelete").write[OnDelete.Value] and
      (JsPath \ "modelBOnDelete").write[OnDelete.Value]
  )(unlift(Relation.unapply))

  val relationReads: Reads[Relation] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "modelAId").read[String] and
      (JsPath \ "modelBId").read[String] and
      (JsPath \ "modelAOnDelete").readNullable[OnDelete.Value].map(_.getOrElse(OnDelete.SetNull)) and
      (JsPath \ "modelBOnDelete").readNullable[OnDelete.Value].map(_.getOrElse(OnDelete.SetNull))
  )(Relation.apply _)

  implicit lazy val relation                  = Format(relationReads, relationWrites)
  implicit lazy val enum                      = Json.format[Enum]
  implicit lazy val field                     = Json.format[Field]
  implicit lazy val model                     = Json.format[Model]
  implicit lazy val schemaFormat              = Json.format[Schema]
  implicit lazy val projectFormat             = Json.format[Project]
  implicit lazy val projectWithClientIdFormat = Json.format[ProjectWithClientId]
  implicit lazy val migrationStatusFormat     = JsonUtils.enumFormat(MigrationStatus)
  implicit lazy val migrationStepsFormat      = Json.format[Migration]

  def failingFormat[T] = new Format[T] {

    override def reads(json: JsValue) = fail
    override def writes(o: T)         = fail

    def fail = sys.error("This JSON Formatter always fails.")
  }

}
