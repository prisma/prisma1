package com.prisma.api.mutations

import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.mutactions.DatabaseMutactions
import com.prisma.api.schema.{APIErrors, ObjectTypeBuilder}
import com.prisma.shared.models.{Model, Project}
import com.prisma.util.coolArgs.CoolArgs
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

case class Delete(
    model: Model,
    modelObjectTypes: ObjectTypeBuilder,
    project: Project,
    args: schema.Args,
    selectedFields: SelectedFields,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {
  var deletedItemOpt: Option[PrismaNode] = None
  val coolArgs                           = CoolArgs(args.raw)
  val where: NodeSelector                = coolArgs.extractNodeSelectorFromWhereField(model)

  override def prepareMutactions(): Future[TopLevelDatabaseMutaction] = {
    dataResolver
      .getNodeByWhere(where, selectedFields)
      .andThen {
        case Success(x) => deletedItemOpt = x
      }
      .map { _ =>
        val itemToDelete = deletedItemOpt.getOrElse(throw APIErrors.NodeNotFoundForWhereError(where))
        val mutaction    = DatabaseMutactions(project).getMutactionsForDelete(where, itemToDelete)

        mutaction
      }
  }

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = Future.successful(ReturnValue(deletedItemOpt.get))
}
