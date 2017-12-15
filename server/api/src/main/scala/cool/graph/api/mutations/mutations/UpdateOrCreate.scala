package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.MutactionGroup
import cool.graph.api.mutations.{ClientMutation, ReturnValueResult}
import cool.graph.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
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

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    for {
      item            <- updateMutation.dataItem
      mutactionGroups <- if (item.isDefined) updateMutation.prepareMutactions() else createMutation.prepareMutactions()
    } yield {
      mutactionGroups
    }
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    updateMutation.dataItem.flatMap {
      case Some(_) => updateMutation.getReturnValue
      case None    => createMutation.getReturnValue
    }
  }
}
