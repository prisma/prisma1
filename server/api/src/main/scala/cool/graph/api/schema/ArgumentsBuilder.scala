package cool.graph.api.schema

import cool.graph.shared.models.{Model, Project}
import cool.graph.util.coolSangria.FromInputImplicit
import sangria.schema._

case class ArgumentsBuilder(project: Project) {

  val inputTypesBuilder: InputTypesBuilder = CachedInputTypesBuilder(project)

  implicit val anyFromInput = FromInputImplicit.CoercedResultMarshaller

  def getSangriaArgumentsForCreate(model: Model): List[Argument[Any]] = {
    val inputObjectType = inputTypesBuilder.inputObjectTypeForCreate(model)
    List(Argument[Any]("data", inputObjectType))
  }

  def getSangriaArgumentsForUpdate(model: Model): Option[List[Argument[Any]]] = {
    val inputObjectType = inputTypesBuilder.inputObjectTypeForUpdate(model)
    whereArgument(model).map { whereArg =>
      List(Argument[Any]("data", inputObjectType), whereArg)
    }
  }

  def getSangriaArgumentsForUpsert(model: Model): Option[List[Argument[Any]]] = {
    whereArgument(model).map { whereArg =>
      List(
        whereArg,
        Argument[Any]("create", inputTypesBuilder.inputObjectTypeForCreate(model)),
        Argument[Any]("update", inputTypesBuilder.inputObjectTypeForUpdate(model))
      )
    }
  }

  def getSangriaArgumentsForDelete(model: Model): Option[List[Argument[Any]]] = {
    whereArgument(model).map(List(_))
  }

  def whereArgument(model: Model): Option[Argument[Any]] =
    inputTypesBuilder.inputObjectTypeForWhere(model).map(inputType => Argument[Any](name = "where", argumentType = inputType))
}
