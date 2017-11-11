package cool.graph.relay.schema

import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import cool.graph.util.coolSangria.FromInputImplicit
import cool.graph.{ArgumentSchema, SchemaArgument}
import sangria.schema.{Args, Argument, InputField, InputObjectType}

object RelayArgumentSchema extends ArgumentSchema {

  implicit val anyFromInput = FromInputImplicit.CoercedResultMarshaller

  val inputObjectName       = "input"
  val clientMutationIdField = InputField("clientMutationId", sangria.schema.StringType)

  override def inputWrapper: Option[String] = Some(inputObjectName)

  override def convertSchemaArgumentsToSangriaArguments(argumentGroupName: String, arguments: List[SchemaArgument]): List[Argument[Any]] = {
    val inputFields     = arguments.map(_.asSangriaInputField)
    val inputObjectType = InputObjectType(argumentGroupName + "Input", inputFields :+ clientMutationIdField)
    val argument        = Argument[Any](name = inputObjectName, argumentType = inputObjectType)
    List(argument)
  }

  override def extractArgumentValues(args: Args, argumentDefinitions: List[SchemaArgument]): List[ArgumentValue] = {
    // Unpack input object.
    // Per definition, we receive an "input" param that contains an object when using relay.
    val argObject: Map[String, Any] = args.raw.get(inputObjectName) match {
      case Some(arg) if arg.isInstanceOf[Map[_, _]] =>
        arg.asInstanceOf[Map[String, Any]]
      case Some(arg) =>
        throw new IllegalArgumentException(s"Expected a map but was: ${arg.getClass}")
      // due to the nested mutation api we need to allow this,
      // as the nested mutation api is removing the "input" for nested models
      case None =>
        args.raw
    }

    val results = argumentDefinitions
      .filter(a => argObject.contains(a.name))
      .map(a => {
        val value = argObject.get(a.name) match {
          case Some(Some(v)) => v
          case Some(v)       => v
          case v             => v
        }
        val argName = a.field.map(_.name).getOrElse(a.name)
        ArgumentValue(argName, value, a.field)
      })

    results
  }
}
