package cool.graph.system.schema.fields

import cool.graph.system.mutations.AddRelationInput
import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput}
import sangria.schema.{OptionInputType, _}

object AddRelation {
  val inputFields = List(
    InputField("projectId", IDType, description = ""),
    InputField("leftModelId", IDType, description = ""),
    InputField("rightModelId", IDType, description = ""),
    InputField("fieldOnLeftModelName", StringType, description = ""),
    InputField("fieldOnRightModelName", StringType, description = ""),
    InputField("fieldOnLeftModelIsList", BooleanType, description = ""),
    InputField("fieldOnRightModelIsList", BooleanType, description = ""),
    InputField("fieldOnLeftModelIsRequired", OptionInputType(BooleanType), description = "Defaults to false. Can only be true for non-list relation fields"),
    InputField("fieldOnRightModelIsRequired", OptionInputType(BooleanType), description = "Defaults to false. Can only be true for non-list relation fields"),
    InputField("name", StringType, description = ""),
    InputField("description", OptionInputType(StringType), description = "")
  ).asInstanceOf[List[InputField[Any]]]

  implicit val manual = new FromInput[AddRelationInput] {
    val marshaller = CoercedScalaResultMarshaller.default
    def fromResult(node: marshaller.Node) = {
      val ad = node.asInstanceOf[Map[String, Any]]

      AddRelationInput(
        clientMutationId = ad.get("clientMutationId").flatMap(_.asInstanceOf[Option[String]]),
        projectId = ad("projectId").asInstanceOf[String],
        leftModelId = ad("leftModelId").asInstanceOf[String],
        rightModelId = ad("rightModelId").asInstanceOf[String],
        fieldOnLeftModelName = ad("fieldOnLeftModelName").asInstanceOf[String],
        fieldOnRightModelName = ad("fieldOnRightModelName").asInstanceOf[String],
        fieldOnLeftModelIsList = ad("fieldOnLeftModelIsList").asInstanceOf[Boolean],
        fieldOnRightModelIsList = ad("fieldOnRightModelIsList").asInstanceOf[Boolean],
        fieldOnLeftModelIsRequired = ad.get("fieldOnLeftModelIsRequired").flatMap(_.asInstanceOf[Option[Boolean]]).getOrElse(false),
        fieldOnRightModelIsRequired = ad.get("fieldOnRightModelIsRequired").flatMap(_.asInstanceOf[Option[Boolean]]).getOrElse(false),
        name = ad("name").asInstanceOf[String],
        description = ad.get("description").flatMap(_.asInstanceOf[Option[String]])
      )
    }
  }
}
