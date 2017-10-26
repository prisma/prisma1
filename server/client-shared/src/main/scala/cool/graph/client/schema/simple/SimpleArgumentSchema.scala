package cool.graph.client.schema.simple

import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import cool.graph.util.coolSangria.FromInputImplicit
import cool.graph.{ArgumentSchema, SchemaArgument}
import sangria.schema.{Args, Argument}

object SimpleArgumentSchema extends ArgumentSchema {

  implicit val anyFromInput = FromInputImplicit.CoercedResultMarshaller

  override def convertSchemaArgumentsToSangriaArguments(argumentGroupName: String, args: List[SchemaArgument]): List[Argument[Any]] = {
    args.map(_.asSangriaArgument)
  }

  override def extractArgumentValues(args: Args, argumentDefinitions: List[SchemaArgument]): List[ArgumentValue] = {
    argumentDefinitions
      .filter(a => args.raw.contains(a.name))
      .map { a =>
        val value = args.raw.get(a.name) match {
          case Some(Some(v)) => v
          case Some(v)       => v
          case v             => v
        }
        val argName = a.field.map(_.name).getOrElse(a.name)
        ArgumentValue(argName, value, a.field)
      }
  }
}
