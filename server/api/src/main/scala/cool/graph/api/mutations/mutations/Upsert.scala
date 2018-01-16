package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.mutactions.UpsertDataItem
import cool.graph.api.database.mutactions.{MutactionGroup, TransactionMutaction}
import cool.graph.api.mutations._
import cool.graph.api.schema.APIErrors
import cool.graph.cuid.Cuid
import cool.graph.shared.models.{Model, Project}
import cool.graph.util.gc_value.GCValueExtractor
import sangria.schema

import scala.concurrent.Future

case class Upsert(
    model: Model,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver,
    allowSettingManagedFields: Boolean = false
)(implicit apiDependencies: ApiDependencies)
    extends SingleItemClientMutation {

  import apiDependencies.system.dispatcher

  val where = CoolArgs(args.raw).extractNodeSelectorFromWhereField(model)

  val idOfNewItem = Cuid.createCuid()
  val createArgs  = CoolArgs(UpsertHelper.generateArgumentMapWithDefaultValues(model, args.raw("create").asInstanceOf[Map[String, Any]] + ("id" -> idOfNewItem)))
  val updateArgs  = CoolArgs(args.raw("update").asInstanceOf[Map[String, Any]])
  val upsert      = UpsertDataItem(project, model, createArgs, updateArgs, where)

  override def prepareMutactions(): Future[List[MutactionGroup]] = {
    val transaction = TransactionMutaction(List(upsert), dataResolver)
    Future.successful(List(MutactionGroup(List(transaction), async = false)))
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    val newWhere = updateArgs.raw.get(where.field.name) match {
      case Some(_) => updateArgs.extractNodeSelector(model)
      case None    => where
    }

    val uniques = Vector(NodeSelector.forId(model, idOfNewItem), newWhere)
    dataResolver.resolveByUniques(model, uniques).map { items =>
      items.headOption match {
        case Some(item) => ReturnValue(item)
        case None       => sys.error("Could not find an item after an Upsert. This should not be possible.") // Todo: what should we do here?
      }
    }
  }
}

object UpsertHelper {
  def generateArgumentMapWithDefaultValues(model: Model, values: Map[String, Any]): Map[String, Any] = {
    model.scalarNonListFields.flatMap { field =>
      values.get(field.name) match {
        case Some(None) if field.defaultValue.isDefined && field.isRequired => throw APIErrors.InputInvalid("null", field.name, model.name)
        case Some(value)                                                    => Some((field.name, value))
        case None if field.defaultValue.isDefined                           => Some((field.name, GCValueExtractor.fromGCValue(field.defaultValue.get)))
        case None                                                           => None
      }
    }.toMap
  }
}
