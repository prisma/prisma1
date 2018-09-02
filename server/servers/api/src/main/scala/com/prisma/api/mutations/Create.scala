package com.prisma.api.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.api.ApiDependencies
import com.prisma.api.connector._
import com.prisma.api.mutactions.DatabaseMutactions
import com.prisma.shared.models._
import com.prisma.util.coolArgs.CoolArgs
import sangria.schema

import scala.concurrent.Future

case class Create(
    model: Model,
    project: Project,
    args: schema.Args,
    selectedFields: SelectedFields,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  val coolArgs: CoolArgs = CoolArgs.fromSchemaArgs(args.raw)

  def prepareMutactions(): Future[TopLevelDatabaseMutaction] = Future.successful {
    DatabaseMutactions(project).getMutactionsForCreate(model, coolArgs)
  }

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = {
    val createdItem = results.databaseResult.asInstanceOf[CreateNodeResult]
    returnValueByUnique(NodeSelector.forIdGCValue(model, createdItem.id), selectedFields)
  }
}
