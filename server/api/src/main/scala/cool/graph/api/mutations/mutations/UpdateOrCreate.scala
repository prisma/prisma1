package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.database.mutactions.mutactions.UpsertDataItem
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.mutations._
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

  import apiDependencies.system.dispatcher

  val updateMutation: Update = Update(model, project, args, dataResolver, argsField = "update")
  val createMutation: Create = Create(model, project, args, dataResolver, argsField = "create")

  println(args.raw)
  val idOfNewItem = Cuid.createCuid()
  val where       = CoolArgs(args.raw).extractNodeSelectorFromWhereField(model)
  val createMap   = args.raw("create").asInstanceOf[Map[String, Any]]
  val createArgs  = CoolArgs(createMap + ("id" -> idOfNewItem))
  val updateArgs  = CoolArgs(args.raw("update").asInstanceOf[Map[String, Any]])

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
//    for {
//      item            <- updateMutation.dataItem
//      mutactionGroups <- if (item.isDefined) updateMutation.prepareMutactions() else createMutation.prepareMutactions()
//    } yield {
//      mutactionGroups
//    }
    val upsert      = UpsertDataItem(project, model, createArgs, updateArgs, where)
    val transaction = Transaction(List(upsert), dataResolver)
    Future.successful(List(MutactionGroup(List(transaction), async = false)))
  }

  override def getReturnValue: Future[ReturnValueResult] = {
//    returnValueById(model, idOfNewItem)
    resolveReturnValue("id", idOfNewItem).flatMap {
      case x: ReturnValue   => Future.successful(x)
      case x: NoReturnValue => resolveReturnValue(where.fieldName, where.fieldValue)
    }
  }

  def resolveReturnValue(field: String, value: Any): Future[ReturnValueResult] = {
    dataResolver.resolveByUnique(model, field, value).map {
      case Some(dataItem) => ReturnValue(dataItem)
      case None           => NoReturnValue(value.toString)
    }
  }
}
