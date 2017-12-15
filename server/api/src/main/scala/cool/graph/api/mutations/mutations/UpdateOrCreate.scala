package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.mutactions.UpsertDataItem
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.mutations.{ClientMutation, CoolArgs, ReturnValueResult}
import cool.graph.cuid.Cuid
import cool.graph.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.Future

case class UpdateOrCreate(
    model: Model,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver,
    allowSettingManagedFields: Boolean = false
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation {

  val updateMutation: Update = Update(model, project, args, dataResolver, argsField = "update")
  val createMutation: Create = Create(model, project, args, dataResolver, argsField = "create")

  val idOfNewItem = Cuid.createCuid()

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
//    for {
//      item            <- updateMutation.dataItem
//      mutactionGroups <- if (item.isDefined) updateMutation.prepareMutactions() else createMutation.prepareMutactions()
//    } yield {
//      mutactionGroups
//    }

    println(args.raw)
    val coolArgs    = CoolArgs(args.raw)
    val createMap   = args.raw("create").asInstanceOf[Map[String, Any]]
    val createArgs  = CoolArgs(createMap + ("id" -> idOfNewItem))
    val updateArgs  = CoolArgs(args.raw("update").asInstanceOf[Map[String, Any]])
    val upsert      = UpsertDataItem(project, model, createArgs, updateArgs, coolArgs.extractNodeSelectorFromWhereField(model))
    val transaction = Transaction(List(upsert), dataResolver)
    Future.successful(List(MutactionGroup(List(transaction), async = false)))
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    returnValueById(model, idOfNewItem)
  }
}
