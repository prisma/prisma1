package cool.graph.api.mutations.definitions

import cool.graph.api.schema.{SchemaArgument, SchemaBuilderUtils}
import cool.graph.gc_values.GCValue
import cool.graph.shared.models.Model
import cool.graph.util.gc_value.GCAnyConverter
import sangria.schema.{Argument, InputField, InputObjectType}

trait ClientMutationDefinition {
  // TODO: there should be no need to override this one. It should be final. We should not override this one.
  def getSangriaArguments(model: Model): List[Argument[Any]] = getSchemaArguments(model).map(_.asSangriaArgument)

  def getSchemaArguments(model: Model): List[SchemaArgument]

  def getWhereArgument(model: Model) = {
    Argument(
      name = "where",
      argumentType = InputObjectType(
        name = s"${model.name}WhereUniqueInput",
        fields = model.fields.filter(_.isUnique).map(field => InputField(name = field.name, fieldType = SchemaBuilderUtils.mapToOptionalInputType(field)))
      )
    )
  }

  def extractNodeSelectorFromSangriaArgs(model: Model, args: sangria.schema.Args): NodeSelector = {
    val whereArgs = args.arg[Map[String, Option[Any]]]("where")
    whereArgs.collectFirst {
      case (fieldName, Some(value)) =>
        NodeSelector(fieldName, GCAnyConverter(model.getFieldByName_!(fieldName).typeIdentifier, isList = false).toGCValue(value).get)
    } getOrElse {
      sys.error("You must specify a unique selector")
    }
  }
}

trait CreateOrUpdateMutationDefinition extends ClientMutationDefinition {
  final def getSchemaArguments(model: Model): List[SchemaArgument] = getScalarArguments(model) ++ getRelationArguments(model)

  def getScalarArguments(model: Model): List[SchemaArgument]

  def getRelationArguments(model: Model): List[SchemaArgument]
}

// note: Below is a SingleFieldNodeSelector. In the future we will also need a MultiFieldNodeSelector
case class NodeSelector(fieldName: String, fieldValue: GCValue)
//object NodeSelector {
//  def fromMap(rawBy: Map[String, Any]) = rawBy.toList.headOption.map(pair => NodeSelector(fieldName = pair._1, fieldValue = GCConver pair._2)).get
//}
