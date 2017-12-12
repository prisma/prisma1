package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.database.mutactions.mutactions.{ServerSideSubscription, UpdateDataItem}
import cool.graph.api.database.mutactions.{ClientSqlMutaction, MutactionGroup, Transaction}
import cool.graph.api.mutations._
import cool.graph.api.mutations.definitions.{NodeSelector, UpdateDefinition}
import cool.graph.api.schema.{APIErrors, InputTypesBuilder}
import cool.graph.shared.models.{Model, Project}
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Update(model: Model, project: Project, args: schema.Args, dataResolver: DataResolver, where: NodeSelector)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation(model, args, dataResolver) {

  override val mutationDefinition = UpdateDefinition(project, InputTypesBuilder(project))

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("data") match { // TODO: input token is probably relay specific?
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }
    CoolArgs(argsPointer)
  }

  lazy val dataItem: Future[Option[DataItem]] = dataResolver.resolveByUnique(model, where.fieldName, where.fieldValue)

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    dataItem map {
      case Some(dataItem) =>
        val validatedDataItem = dataItem // todo: use GC Values
        // = dataItem.copy(userData = GraphcoolDataTypes.fromSql(dataItem.userData, model.fields))

        val sqlMutactions: List[ClientSqlMutaction] =
          SqlMutactions(dataResolver).getMutactionsForUpdate(project, model, coolArgs, dataItem.id, validatedDataItem)

        val transactionMutaction = Transaction(sqlMutactions, dataResolver)

        val updateMutactionOpt: Option[UpdateDataItem] = sqlMutactions.collect { case x: UpdateDataItem => x }.headOption

        val updateMutactions = sqlMutactions.collect { case x: UpdateDataItem => x }

        val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, sqlMutactions).toList

        val sssActions = ServerSideSubscription.extractFromMutactions(project, sqlMutactions, requestId = "").toList

        List(
          MutactionGroup(mutactions = List(transactionMutaction), async = false),
          MutactionGroup(mutactions = sssActions ++ subscriptionMutactions, async = true)
        )

      case None =>
        throw APIErrors.DataItemDoesNotExist(model.name, where.fieldName, where.fieldValue.toString)
    }
  }

  override def getReturnValue: Future[ReturnValueResult] = {
    dataItem flatMap {
      case Some(dataItem) => returnValueById(model, dataItem.id)
      case None           => Future.successful(NoReturnValue(where.fieldValue.toString)) // FIXME: NoReturnValue should not be fixed to id only.
    }
  }

}
