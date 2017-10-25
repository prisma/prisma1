package cool.graph

import cool.graph.shared.models.Field
import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import cool.graph.util.coolSangria.FromInputImplicit
import sangria.schema.{Args, Argument, InputField, InputType}

trait ArgumentSchema {
  def inputWrapper: Option[String] = None

  def convertSchemaArgumentsToSangriaArguments(argumentGroupName: String, arguments: List[SchemaArgument]): List[Argument[Any]]

  def extractArgumentValues(args: Args, argumentDefinitions: List[SchemaArgument]): List[ArgumentValue]
}

/**
  * just a sketch of how things could work
case class SchemaArgumentsGroup(name: String, arguments: List[SchemaArgument]) {
  def convertToSangriaArguments(argumentSchema: ArgumentSchema) = {
    argumentSchema.convertSchemaArgumentsToSangriaArguments(name, arguments)
  }
}*/
case class SchemaArgument(name: String, inputType: InputType[Any], description: Option[String], field: Option[Field] = None) {
  import FromInputImplicit.CoercedResultMarshaller

  lazy val asSangriaInputField = InputField(name, inputType, description.getOrElse(""))
  lazy val asSangriaArgument   = Argument.createWithoutDefault(name, inputType, description)
}

object SchemaArgument {
  def apply(name: String, inputType: InputType[Any], description: Option[String], field: Field): SchemaArgument = {
    SchemaArgument(name, inputType, description, Some(field))
  }

  def apply(name: String, inputType: InputType[Any]): SchemaArgument = {
    SchemaArgument(name, inputType, None, None)
  }
}
/**
  * just another sketch of how things could work
sealed trait MyArgType
case class FlatType(name: String, tpe: MyArgType)              extends MyArgType
case class GroupType(groupName: String, args: List[MyArgType]) extends MyArgType
case class LeafType(name: String, tpe: TypeIdentifier.Value)   extends MyArgType
  */
