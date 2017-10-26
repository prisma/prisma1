package cool.graph.system.schema.fields

import cool.graph.shared.models.FieldConstraintType.FieldConstraintType
import cool.graph.system.mutations.AddFieldConstraintInput
import cool.graph.system.schema.types.FieldConstraintTypeType
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{BooleanType, FloatType, IDType, InputField, IntType, ListInputType, OptionInputType, StringType}

object AddFieldConstraint {
  val inputFields =
    List(
      InputField("fieldId", IDType, description = ""),
      InputField("constraintType", FieldConstraintTypeType.Type, description = ""),
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

  implicit val manual = new FromInput[AddFieldConstraintInput] {
    import cool.graph.util.coolSangria.ManualMarshallerHelpers._

    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      AddFieldConstraintInput(
        clientMutationId = node.clientMutationId,
        fieldId = node.requiredArgAsString("fieldId"),
        constraintType = node.requiredArgAs[FieldConstraintType]("constraintType"),
        equalsString = node.optionalArgAs[String]("equalsString"),
        oneOfString = node.optionalArgAs[Seq[String]]("oneOfString"),
        minLength = node.optionalArgAs[Int]("minLength"),
        maxLength = node.optionalArgAs[Int]("maxLength"),
        startsWith = node.optionalArgAs[String]("startsWith"),
        endsWith = node.optionalArgAs[String]("endsWith"),
        includes = node.optionalArgAs[String]("includes"),
        regex = node.optionalArgAs[String]("regex"),
        equalsNumber = node.optionalArgAs[Double]("equalsNumber"),
        oneOfNumber = node.optionalArgAs[Seq[Double]]("oneOfNumber"),
        min = node.optionalArgAs[Double]("min"),
        max = node.optionalArgAs[Double]("max"),
        exclusiveMin = node.optionalArgAs[Double]("exclusiveMin"),
        exclusiveMax = node.optionalArgAs[Double]("exclusiveMax"),
        multipleOf = node.optionalArgAs[Double]("multipleOf"),
        equalsBoolean = node.optionalArgAs[Boolean]("equalsBoolean"),
        uniqueItems = node.optionalArgAs[Boolean]("uniqueItems"),
        minItems = node.optionalArgAs[Int]("minItems"),
        maxItems = node.optionalArgAs[Int]("maxItems")
      )
    }
  }
}
