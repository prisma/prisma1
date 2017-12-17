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

  def getSangriaArgumentsForUpdate(model: Model): List[Argument[Any]] = {
    val inputObjectType = inputTypesBuilder.inputObjectTypeForUpdate(model)
    List(Argument[Any]("data", inputObjectType), whereArgument(model))
  }

  def getSangriaArgumentsForUpsert(model: Model): List[Argument[Any]] = {
    List(
      whereArgument(model),
      Argument[Any]("create", inputTypesBuilder.inputObjectTypeForCreate(model)),
      Argument[Any]("update", inputTypesBuilder.inputObjectTypeForUpdate(model))
    )
  }

  def getSangriaArgumentsForDelete(model: Model): List[Argument[Any]] = {
    List(whereArgument(model))
  }

  def whereArgument(model: Model) = Argument[Any](name = "where", argumentType = inputTypesBuilder.inputObjectTypeForWhere(model))
}
