package cool.graph.api.schema

import cool.graph.shared.models.{Model, Project}
import cool.graph.util.coolSangria.FromInputImplicit
import sangria.schema._

case class ArgumentsBuilder(project: Project) {

  val inputTypesBuilder: InputTypesBuilder = CachedInputTypesBuilder(project)

  implicit val anyFromInput = FromInputImplicit.CoercedResultMarshaller

  def getSangriaArgumentsForCreate(model: Model): Option[List[Argument[Any]]] = {
    inputTypesBuilder.inputObjectTypeForCreate(model).map { args =>
      List(Argument[Any]("data", args))
    }
  }

  def getSangriaArgumentsForUpdate(model: Model): Option[List[Argument[Any]]] = {
    for {
      whereArg <- whereUniqueArgument(model)
      dataArg  <- inputTypesBuilder.inputObjectTypeForUpdate(model)
    } yield {
      List(
        Argument[Any]("data", dataArg),
        whereArg
      )
    }
  }

  def getSangriaArgumentsForUpsert(model: Model): Option[List[Argument[Any]]] = {
    for {
      whereArg  <- whereUniqueArgument(model)
      createArg <- inputTypesBuilder.inputObjectTypeForCreate(model)
      updateArg <- inputTypesBuilder.inputObjectTypeForUpdate(model)
    } yield {
      List(
        whereArg,
        Argument[Any]("create", createArg),
        Argument[Any]("update", updateArg)
      )
    }
  }

  def getSangriaArgumentsForDelete(model: Model): Option[List[Argument[Any]]] = {
    whereUniqueArgument(model).map(List(_))
  }

  def getSangriaArgumentsForUpdateMany(model: Model): Option[List[Argument[Any]]] = {
    inputTypesBuilder.inputObjectTypeForUpdate(model).map { updateArg =>
      List(
        Argument[Any]("data", updateArg),
        whereArgument(model)
      )
    }
  }

  def getSangriaArgumentsForDeleteMany(model: Model): List[Argument[Any]] = List(whereArgument(model))

  def whereArgument(model: Model) = Argument[Any](name = "where", argumentType = inputTypesBuilder.inputObjectTypeForWhere(model))

  def whereUniqueArgument(model: Model): Option[Argument[Any]] = {
    inputTypesBuilder.inputObjectTypeForWhereUnique(model).map(inputType => Argument[Any](name = "where", argumentType = inputType))
  }

}
