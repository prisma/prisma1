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

  val coolArgs: CoolArgs   = CoolArgs.fromSchemaArgs(args.raw)
  lazy val createMutaction = DatabaseMutactions(project).getMutactionsForCreate(model, coolArgs)

  def prepareMutactions(): Future[TopLevelDatabaseMutaction] = Future.successful { createMutaction }

  override def getReturnValue(results: MutactionResults): Future[ReturnValueResult] = {
    val createdItem = results.results.collectFirst { case r: CreateNodeResult if r.mutaction.id == createMutaction.id => r }.get
    returnValueByUnique(NodeSelector.forId(model, createdItem.id), selectedFields)
  }
}
