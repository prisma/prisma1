package cool.graph.system.schema.fields

import cool.graph.JsonFormats
import cool.graph.shared.schema.JsonMarshalling.CustomSprayJsonResultMarshaller
import cool.graph.system.mutations.UpdateFieldConstraintInput
import sangria.marshalling.FromInput
import sangria.schema.{BooleanType, FloatType, IDType, InputField, IntType, ListInputType, OptionInputType, ScalarType, StringType}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsBoolean, JsNull, _}

object UpdateFieldConstraint {
  val inputFields =
    List(
      InputField("constraintId", IDType, description = ""),
      InputField("equalsString", OptionInputType(StringType), description = ""),
      InputField("oneOfString", OptionInputType(ListInputType(StringType)), description = ""),
      InputField("minLength", OptionInputType(IntType), description = ""),
      InputField("maxLength", OptionInputType(IntType), description = ""),
      InputField("startsWith", OptionInputType(StringType), description = ""),
      InputField("endsWith", OptionInputType(StringType), description = ""),
      InputField("includes", OptionInputType(StringType), description = ""),
      InputField("regex", OptionInputType(StringType), description = ""),
      InputField("equalsNumber", OptionInputType(FloatType), description = ""),
      InputField("oneOfNumber", OptionInputType(ListInputType(FloatType)), description = ""),
      InputField("min", OptionInputType(FloatType), description = ""),
      InputField("max", OptionInputType(FloatType), description = ""),
      InputField("exclusiveMin", OptionInputType(FloatType), description = ""),
      InputField("exclusiveMax", OptionInputType(FloatType), description = ""),
      InputField("multipleOf", OptionInputType(FloatType), description = ""),
      InputField("equalsBoolean", OptionInputType(BooleanType), description = ""),
      InputField("uniqueItems", OptionInputType(BooleanType), description = ""),
      InputField("minItems", OptionInputType(IntType), description = ""),
      InputField("maxItems", OptionInputType(IntType), description = "")
    ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[UpdateFieldConstraintInput] {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    val marshaller         = CustomSprayJsonResultMarshaller
    def fromResult(node: marshaller.Node): UpdateFieldConstraintInput = {

      def tripleOption(name: String): Option[Option[Any]] = {

        if (node.asJsObject.getFields(name).nonEmpty) {
          node.asJsObject.getFields(name).head match {
            case JsNull       => Some(None)
            case b: JsBoolean => Some(Some(b.value))
            case n: JsNumber  => Some(Some(n.convertTo[Double]))
            case s: JsString  => Some(Some(s.convertTo[String]))
            case a: JsArray =>
              Some(
                Some(
                  a.convertTo[List[JsValue]]
                    .map {
                      case b: JsBoolean => b.convertTo[Boolean]
                      case n: JsNumber  => n.convertTo[Double]
                      case s: JsString  => s.convertTo[String]
                      case _            =>
                    }
                ))
            case _ => None
          }
        } else None
      }

      def tripleOptionInt(name: String): Option[Option[Int]] = {

        if (node.asJsObject.getFields(name).nonEmpty) {
          node.asJsObject.getFields(name).head match {
            case JsNull      => Some(None)
            case n: JsNumber => Some(Some(n.convertTo[Int]))
            case _           => None
          }
        } else None
      }

      def getAsString(name: String) = node.asJsObject.getFields(name).head.asInstanceOf[JsString].convertTo[String]

      UpdateFieldConstraintInput(
        clientMutationId = tripleOption("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        constraintId = getAsString("constraintId"),
        equalsString = tripleOption("equalsString"),
        oneOfString = tripleOption("oneOfString"),
        minLength = tripleOptionInt("minLength"),
        maxLength = tripleOptionInt("maxLength"),
        startsWith = tripleOption("startsWith"),
        endsWith = tripleOption("endsWith"),
        includes = tripleOption("includes"),
        regex = tripleOption("regex"),
        equalsNumber = tripleOption("equalsNumber"),
        oneOfNumber = tripleOption("oneOfNumber"),
        min = tripleOption("min"),
        max = tripleOption("max"),
        exclusiveMin = tripleOption("exclusiveMin"),
        exclusiveMax = tripleOption("exclusiveMax"),
        multipleOf = tripleOption("multipleOf"),
        equalsBoolean = tripleOption("equalsBoolean"),
        uniqueItems = tripleOption("uniqueItems"),
        minItems = tripleOptionInt("minItems"),
        maxItems = tripleOptionInt("maxItems")
      )
    }
  }
}
