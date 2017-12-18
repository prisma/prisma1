package cool.graph.api.database.mutactions.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.api.database.mutactions.validation.InputValueValidation
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import cool.graph.api.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.api.mutations.{CoolArgs, NodeSelector}
import cool.graph.api.schema.APIErrors
import cool.graph.shared.models.{Model, Project}
import slick.dbio.DBIOAction

import scala.concurrent.Future
import scala.util.{Success, Try}

case class UpsertDataItem(
    project: Project,
    model: Model,
    createArgs: CoolArgs,
    updateArgs: CoolArgs,
    where: NodeSelector
) extends ClientSqlDataChangeMutaction {

//  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
//    val updateAction = DatabaseMutationBuilder.updateDataItemByUnique(project, model, updateArgs, where)
//    val createAction = DatabaseMutationBuilder.createDataItemIfUniqueDoesNotExist(project, model, createArgs, where)
//    ClientSqlStatementResult(DBIOAction.seq(updateAction, createAction))
//  }

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    ClientSqlStatementResult(DatabaseMutationBuilder.upsert(project,model,createArgs,updateArgs,where))
  }

  override def handleErrors = Some({ case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1048 => APIErrors.FieldCannotBeNull()})

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {
    val (createCheck, _) = InputValueValidation.validateDataItemInputs(model, createArgs.scalarArguments(model).toList)
    val (updateCheck, _) = InputValueValidation.validateDataItemInputs(model, updateArgs.scalarArguments(model).toList)

    (createCheck.isFailure, updateCheck.isFailure) match {
      case (true, _) => Future.successful(createCheck)
      case (_, true) => Future.successful(updateCheck)
      case (false, false) => Future.successful(Success(MutactionVerificationSuccess()))
    }
  }
}
