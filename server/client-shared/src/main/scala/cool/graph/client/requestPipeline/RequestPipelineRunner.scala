package cool.graph.client.requestPipeline

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.DataItem
import cool.graph.client.adapters.GraphcoolDataTypes
import cool.graph.client.mutactions.validation.InputValueValidation
import cool.graph.client.mutations.CoolArgs
import cool.graph.client.schema.SchemaModelObjectTypesBuilder
import cool.graph.shared.errors.UserAPIErrors.FieldCannotBeNull
import cool.graph.shared.functions.EndpointResolver
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.shared.models._
import cool.graph.shared.mutactions.MutationTypes.{ArgumentValue, ArgumentValueList}
import org.joda.time.{DateTime, DateTimeZone}
import scaldi.{Injectable, Injector}
import spray.json.{DefaultJsonProtocol, JsArray, JsBoolean, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, JsonFormat}

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class RequestPipelineRunner(requestId: String)(implicit val inj: Injector) extends Injectable {
  implicit val system: ActorSystem             = inject[_root_.akka.actor.ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[_root_.akka.stream.ActorMaterializer](identified by "actorMaterializer")

  // Transform arguments by executing function
  // original values are returned if no data returned by function
  def runTransformArgument(project: Project,
                           model: Model,
                           operation: RequestPipelineOperation,
                           values: List[ArgumentValue],
                           originalArgs: Option[CoolArgs]): Future[List[ArgumentValue]] = {
    val appliedFn = project.requestPipelineFunctionForModel(model, FunctionBinding.TRANSFORM_ARGUMENT, operation)

    val checkRequiredFields = operation == RequestPipelineOperation.CREATE
    executeFunction(project, model, appliedFn, values, originalArgs, checkRequiredFields)
  }

  // Receives transformed data from TransformArgument
  // Returned data is ignored, but errors halts the request
  def runPreWrite(project: Project,
                  model: Model,
                  operation: RequestPipelineOperation,
                  values: List[ArgumentValue],
                  originalArgsOpt: Option[CoolArgs]): Future[Boolean] = {
    val function = project.requestPipelineFunctionForModel(model, FunctionBinding.PRE_WRITE, operation)

    val transformedOriginalArgsOpt = originalArgsOpt.map(originalArgs => {
      originalArgs.copy(raw = originalArgs.raw.map {
        case (key, value) => (key, values.find(_.name == key).map(_.value).getOrElse(value))
      })
    })

    executeFunction(project, model, function, values, transformedOriginalArgsOpt).map(_ => true)
  }

  // Transform arguments by executing function
  // original values are returned if no data returned by function
  def runTransformPayload(project: Project, model: Model, operation: RequestPipelineOperation, values: List[ArgumentValue]): Future[List[ArgumentValue]] = {
    val appliedFn: Option[RequestPipelineFunction] = project.requestPipelineFunctionForModel(model, FunctionBinding.TRANSFORM_PAYLOAD, operation)
    executeFunction(project, model, appliedFn, values, originalArgs = None)
  }

  def executeFunction(project: Project,
                      model: Model,
                      appliedFn: Option[RequestPipelineFunction],
                      originalValues: List[ArgumentValue],
                      originalArgs: Option[CoolArgs],
                      checkRequiredFields: Boolean = false): Future[List[ArgumentValue]] = {
    appliedFn match {
      case None => Future.successful(originalValues)
      case Some(function) =>
        RpFunctionExecutor(requestId).execute_!(project, model, function, originalValues, originalArgs) map {
          case FunctionSuccess(x, _) if x.isNull =>
            originalValues

          case FunctionSuccess(x, _) =>
            val graphcoolValues   = GraphcoolDataTypes.fromJson(data = x.values.head, fields = model.fields)
            val transformedValues = keepOriginalId(originalValues, model, graphcoolValues)
            val id                = ArgumentValueList.getId_!(originalValues)

            transformedValues.map(arg => {
              val field = model.getFieldByName_!(arg.name)
              InputValueValidation.argumentValueTypeValidation(field, arg.unwrappedValue)
            })

            val (check, _) = InputValueValidation.validateDataItemInputs(model, id, transformedValues)
            if (check.isFailure) throw check.failed.get

            if (checkRequiredFields) {
              val missingRequiredFieldNames: List[String] = InputValueValidation.validateRequiredScalarFieldsHaveValues(model, transformedValues)
              if (missingRequiredFieldNames.nonEmpty) throw FieldCannotBeNull(missingRequiredFieldNames.head)
            }
            transformedValues
        }
    }
  }

  private def keepOriginalId(original: List[ArgumentValue], model: Model, returnValue: Map[String, Option[Any]]): List[ArgumentValue] = {

    val newValues: List[ArgumentValue] = returnValue.map(x => ArgumentValue(x._1, x._2, model.getFieldByName(x._1))).toList

    val onlyScalar: List[ArgumentValue] = newValues.filter(arg => arg.field.exists(_.isScalar))
    val fixedDateTimes = onlyScalar.map(argumentValue => {
      argumentValue.field.exists(_.typeIdentifier == TypeIdentifier.DateTime) match {
        case true =>
          val value = argumentValue.value match {
            case Some(x: String) => Some(new DateTime(x, DateTimeZone.UTC))
            case x               => x
          }
          argumentValue.copy(value = value)
        case false => argumentValue
      }
    })

    val id = original.find(_.name == "id")
    id match {
      case Some(id) => fixedDateTimes.filter(_.name != "id") :+ id
      case None     => fixedDateTimes.filter(_.name != "id")
    }
  }
}

case class RpFunctionExecutor(requestId: String)(implicit val inj: Injector) extends Injectable {

  // GraphQL differentiates between null and undefined
  // null means explicit null, in our case it sets the value to null in the database
  // undefined is an optional argument that was not supplied.
  // This distinction is important in UPDATE mutations
  // In our domain model explicit nulls are modeled as None, omitted arguments are missing from argument list
  private def handleOptionalAndNullValues(value: Any) = {
    value match {
      case Some(x) => x
      case None    => null
      case x       => x
    }
  }
  private def valuesToMap(values: List[ArgumentValue]): ListMap[String, Any] = {
    // note: ListMap preserves ordering
    ListMap(values.map(x => (x.name, handleOptionalAndNullValues(x.value))).sortBy(_._1): _*)
  }
  private def coolArgsToMap(rawArgs: Map[String, Any]): ListMap[String, Any] = {
    // note: ListMap preserves ordering
    ListMap(rawArgs.mapValues(handleOptionalAndNullValues(_)).toList.sortBy(_._1): _*).map {
      case (key, value) if value.isInstanceOf[Vector[_]] =>
        value.asInstanceOf[Vector[Any]] match {
          case value if value.nonEmpty && value.head.isInstanceOf[Map[_, _]] =>
            (key, value.asInstanceOf[Vector[Map[String, Any]]].map(coolArgsToMap))
          case value => (key, value)
        }
      case (key, value) if value.isInstanceOf[Map[_, _]] =>
        (key, coolArgsToMap(value.asInstanceOf[Map[String, Any]]))
      case (key, value) => (key, value)
    }
  }

  def execute_!(project: Project, model: Model, function: RequestPipelineFunction, values: List[ArgumentValue], originalArgs: Option[CoolArgs] = None)(
      implicit inj: Injector): Future[FunctionSuccess] = {
    val functionExecutor = new FunctionExecutor()

    val originalArgsWithId = originalArgs.map { args =>
      values.find(_.name == "id") match {
        case None          => args.raw
        case Some(idValue) => args.raw + ("id" -> idValue.value)
      }
    }

    val endpointResolver          = inject[EndpointResolver](identified by "endpointResolver")
    val context: Map[String, Any] = FunctionExecutor.createEventContext(project, "", headers = Map.empty, None, endpointResolver)

    val argsAndContext = Map(
      "data"    -> originalArgsWithId.map(coolArgsToMap).getOrElse(valuesToMap(values)),
      "context" -> context
    )

    val event = AnyJsonFormat.write(argsAndContext).compactPrint
    functionExecutor.syncWithLoggingAndErrorHandling_!(function, event, project, requestId)
  }

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any): JsValue = x match {
      case m: Map[_, _] =>
        JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
      case l: List[Any]   => JsArray(l.map(write).toVector)
      case l: Vector[Any] => JsArray(l.map(write))
      case l: Seq[Any]    => JsArray(l.map(write).toVector)
      case n: Int         => JsNumber(n)
      case n: Long        => JsNumber(n)
      case n: BigDecimal  => JsNumber(n)
      case n: Double      => JsNumber(n)
      case s: String      => JsString(s)
      case true           => JsTrue
      case false          => JsFalse
      case v: JsValue     => v
      case null           => JsNull
      case r              => JsString(r.toString)
    }

    def read(x: JsValue): Any = {
      x match {
        case l: JsArray   => l.elements.map(read).toList
        case m: JsObject  => m.fields.mapValues(read)
        case s: JsString  => s.value
        case n: JsNumber  => n.value
        case b: JsBoolean => b.value
        case JsNull       => null
        case _            => sys.error("implement all scalar types!")
      }
    }
  }

  implicit lazy val myMapFormat: JsonFormat[Map[String, Any]] = {
    import DefaultJsonProtocol._
    mapFormat[String, Any]
  }
}

object RequestPipelineRunner {
  def dataItemToArgumentValues(dataItem: DataItem, model: Model): List[ArgumentValue] = {
    val args = dataItem.userData
      .flatMap(x => {
        model
          .getFieldByName(x._1)
          .map(field => {
            val value = SchemaModelObjectTypesBuilder.convertScalarFieldValueFromDatabase(field, dataItem)
            ArgumentValue(name = field.name, value = value, field = field)
          })
      })
      .toList :+ ArgumentValue("id", dataItem.id)
    args
  }

  def argumentValuesToDataItem(argumentValues: List[ArgumentValue], id: String, model: Model): DataItem = {
    val dataItem = DataItem(
      id = id,
      userData = argumentValues.collect {
        case x if model.fields.exists(_.name == x.name) =>
          val field = model.getFieldByName_!(x.name)
          (x.name, fromJsValues(normaliseOptions(x.value), field))
      }.toMap,
      typeName = Some(model.name)
    )
    dataItem
  }

  private def normaliseOptions(value: Any): Option[Any] = value match {
    case None       => None
    case null       => None
    case Some(null) => None
    case Some(x)    => Some(x)
    case x          => Some(x)
  }

  // For lists: JsArray    => String
  // For Int:   BigDecimal => Int
  // For Float: BigDecimal => Double
  private def fromJsValues(value: Option[Any], field: Field): Option[Any] = {
    def convertNumberToInt(value: Any): Int = value match {
      case x: BigDecimal => x.toInt
      case x: Float      => x.toInt
      case x: Double     => x.toInt
      case x: Int        => x
    }
    def convertNumberToDouble(value: Any): Double = value match {
      case x: BigDecimal => x.toDouble
      case x: Float      => x.toDouble
      case x: Double     => x
      case x: Int        => x.toDouble
    }

    field.isList match {
      case true =>
        value match {
          case Some(x: JsArray) => Some(x.compactPrint)
          case x                => x
        }

      case false =>
        field.typeIdentifier match {
          case TypeIdentifier.String    => value
          case TypeIdentifier.Int       => value.map(convertNumberToInt)
          case TypeIdentifier.Float     => value.map(convertNumberToDouble)
          case TypeIdentifier.Boolean   => value
          case TypeIdentifier.GraphQLID => value
          case TypeIdentifier.Password  => value
          case TypeIdentifier.DateTime  => value
          case TypeIdentifier.Enum      => value
          case TypeIdentifier.Json      => value
        }
    }
  }
}
