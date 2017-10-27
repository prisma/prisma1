package cool.graph.client.mutactions

import cool.graph.shared.mutactions.MutationTypes.ArgumentValue
import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder, ProjectRelayIdTable}
import cool.graph.client.requestPipeline.RequestPipelineRunner
import cool.graph.shared.NameConstraints
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Model, Project, RequestPipelineOperation}
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteDataItem(project: Project, model: Model, id: Id, previousValues: DataItem, requestId: Option[String] = None)(implicit val inj: Injector)
    extends ClientSqlDataChangeMutaction
    with Injectable {

  val pipelineRunner = new RequestPipelineRunner(requestId.getOrElse(""))

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val relayIds = TableQuery(new ProjectRelayIdTable(_, project.id))

    val values = List(ArgumentValue("id", id, model.getFieldByName_!("id")))
    for {
      transformedValues <- pipelineRunner.runTransformArgument(
                            project = project,
                            model = model,
                            operation = RequestPipelineOperation.DELETE,
                            values = values,
                            originalArgs = None
                          )
      _ <- pipelineRunner.runPreWrite(
            project = project,
            model = model,
            operation = RequestPipelineOperation.DELETE,
            values = transformedValues,
            originalArgsOpt = None
          )
    } yield {
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(DatabaseMutationBuilder.deleteDataItemById(project.id, model.name, id), relayIds.filter(_.id === id).delete))
    }
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    if (!NameConstraints.isValidDataItemId(id))
      return Future.successful(Failure(UserAPIErrors.IdIsInvalid(id)))

    resolver.existsByModelAndId(model, id) map {
      case false => Failure(UserAPIErrors.DataItemDoesNotExist(model.name, id))
      case true  => Success(MutactionVerificationSuccess())
    }
  }
}
