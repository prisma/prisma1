package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.mutactions.mutactions.{ServerSideSubscription, UpdateDataItem}
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.database.mutactions.{ClientSqlMutaction, MutactionGroup, Transaction}
import cool.graph.api.mutations._
import cool.graph.api.mutations.definitions.{NodeSelector, UpdateDefinition}
import cool.graph.api.schema.{APIErrors, InputTypesBuilder}
import cool.graph.gc_values.{GraphQLIdGCValue, StringGCValue}
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Update(model: Model, project: Project, args: schema.Args, dataResolver: DataResolver, by: NodeSelector)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation(model, args, dataResolver) {

  override val mutationDefinition = UpdateDefinition(project, InputTypesBuilder(project))

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("input") match { // TODO: input token is probably relay specific?
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }
    CoolArgs(argsPointer, model, project)
  }

  val id                = by.fieldValue.asInstanceOf[GraphQLIdGCValue].value // todo: pass NodeSelector all the way down
  val requestId: String = "" // dataResolver.requestContext.map(_.requestId).getOrElse("")

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    dataResolver.resolveByUnique(model, by.fieldName, by.fieldValue) map {
      case Some(dataItem) =>
        val validatedDataItem = dataItem // todo: use GC Values
        // = dataItem.copy(userData = GraphcoolDataTypes.fromSql(dataItem.userData, model.fields))

        val sqlMutactions: List[ClientSqlMutaction] =
          SqlMutactions(dataResolver).getMutactionsForUpdate(project, model, coolArgs, id, validatedDataItem, requestId)

        val transactionMutaction = Transaction(sqlMutactions, dataResolver)

        val updateMutactionOpt: Option[UpdateDataItem] = sqlMutactions.collect { case x: UpdateDataItem => x }.headOption

        val updateMutactions = sqlMutactions.collect { case x: UpdateDataItem => x }

        val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions).toList

        val sssActions = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId).toList

        List(
          MutactionGroup(mutactions = List(transactionMutaction), async = false),
          MutactionGroup(mutactions = sssActions ++ subscriptionMutactions, async = true)
        )

      case None =>
        List(
          MutactionGroup(
            mutactions = List(
              UpdateDataItem(project = project,
                             model = model,
                             id = id,
                             values = List.empty,
                             originalArgs = None,
                             previousValues = DataItem(id),
                             itemExists = false)),
            async = false
          ),
          MutactionGroup(mutactions = List.empty, async = true)
        )
    }
  }

  override def getReturnValue: Future[ReturnValue] = {

    def ensureReturnValue(returnValue: ReturnValueResult): ReturnValue = {
      returnValue match {
        case x: NoReturnValue => throw APIErrors.DataItemDoesNotExist(model.name, id)
        case x: ReturnValue   => x
      }
    }

    for {
      returnValueResult <- returnValueById(model, id)
      dataItem          = ensureReturnValue(returnValueResult).dataItem

    } yield {
      ReturnValue(dataItem)
    }
  }
}
