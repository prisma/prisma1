package com.prisma.api.schema

import com.prisma.api.schema.SangriaQueryArguments.whereArgument
import com.prisma.shared.models.{Model, Project}
import com.prisma.util.coolSangria.FromInputImplicit
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
    inputTypesBuilder.inputObjectTypeForUpdate(model).map { updateArg: InputObjectType[Any] =>
      List(Argument[Any]("data", updateArg), whereArgument(model, project).asInstanceOf[Argument[Any]]) //todo this is ugly
    }
  }

  def getSangriaArgumentsForDeleteMany(model: Model): List[Argument[Option[Any]]] = {
    List(whereArgument(model, project))
  }

  def whereUniqueArgument(model: Model): Option[Argument[Any]] = {
    inputTypesBuilder.inputObjectTypeForWhereUnique(model).map(inputType => Argument[Any](name = "where", argumentType = inputType))
  }

}
