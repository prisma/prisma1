package cool.graph.shared.models

import cool.graph.gc_values._
import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import cool.graph.utils.json.JsonUtils._

object ProjectJsonFormatter {

  // ENUMS
  implicit lazy val seatStatus               = enumFormat(SeatStatus)
  implicit lazy val logStatus                = enumFormat(LogStatus)
  implicit lazy val requestPipelineOperation = enumFormat(RequestPipelineOperation)
  implicit lazy val relationSide             = enumFormat(RelationSide)
  implicit lazy val typeIdentifier           = enumFormat(TypeIdentifier)
  implicit lazy val fieldConstraintType      = enumFormat(FieldConstraintType)
  implicit lazy val userType                 = enumFormat(UserType)
  implicit lazy val modelMutationType        = enumFormat(ModelMutationType)
  implicit lazy val customRule               = enumFormat(CustomRule)
  implicit lazy val modelOperation           = enumFormat(ModelOperation)

  // FAILING STUBS
  implicit lazy val function = failingFormat[Function]

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
        val gcValues = elements.map(element => this.createGcValue(discriminator, element, isList = false))
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

  implicit lazy val relationFieldMirror       = Json.format[RelationFieldMirror]
  implicit lazy val relation                  = Json.format[Relation]
  implicit lazy val enum                      = Json.format[Enum]
  implicit lazy val field                     = Json.format[Field]
  implicit lazy val model                     = Json.format[Model]
  implicit lazy val seat                      = Json.format[Seat]
  implicit lazy val featureToggle             = Json.format[FeatureToggle]
  implicit lazy val projectFormat             = Json.format[Project]
  implicit lazy val projectWithClientIdFormat = Json.format[ProjectWithClientId]

  def failingFormat[T] = new Format[T] {

    override def reads(json: JsValue) = fail
    override def writes(o: T)         = fail

    def fail = sys.error("This JSON Formatter always fails.")
  }

}
