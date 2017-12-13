package cool.graph.api.mutations.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.mutactions.CreateDataItem
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.mutations._
import cool.graph.cuid.Cuid
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models._
import sangria.schema

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Create(
    model: Model,
    project: Project,
    args: schema.Args,
    dataResolver: DataResolver
)(implicit apiDependencies: ApiDependencies)
    extends ClientMutation {

  implicit val system: ActorSystem             = apiDependencies.system
  implicit val materializer: ActorMaterializer = apiDependencies.materializer

  val id: Id            = Cuid.createCuid()
  val requestId: String = "" //                        = dataResolver.requestContext.map(_.requestId).getOrElse("")

  val coolArgs: CoolArgs = {
    val argsPointer: Map[String, Any] = args.raw.get("data") match { // TODO: input token is probably relay specific?
      case Some(value) => value.asInstanceOf[Map[String, Any]]
      case None        => args.raw
    }

    CoolArgs(argsPointer)
  }

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    val createMutactionsResult = SqlMutactions(dataResolver).getMutactionsForCreate(project, model, coolArgs, id)

    val transactionMutaction = Transaction(createMutactionsResult.allMutactions.toList, dataResolver)
    val createMutactions     = createMutactionsResult.allMutactions.collect { case x: CreateDataItem => x }

    val subscriptionMutactions = SubscriptionEvents.extractFromSqlMutactions(project, mutationId, createMutactionsResult.allMutactions)
//    val sssActions             = ServerSideSubscription.extractFromMutactions(project, createMutactionsResult.allMutactions, requestId)

    Future.successful(
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false),
        MutactionGroup(mutactions = //sssActions ++
                         subscriptionMutactions.toList,
                       async = true)
      ))

  }

  override def getReturnValue: Future[ReturnValueResult] = {
    for {
      returnValue <- returnValueById(model, id)
      dataItem    = returnValue.asInstanceOf[ReturnValue].dataItem
    } yield {
      ReturnValue(dataItem)
    }
  }
}
