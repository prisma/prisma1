package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlMutaction, ClientSqlStatementResult}

import scala.concurrent.Future

case class EnableForeignKeyConstraintChecks() extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] =
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.enableForeignKeyConstraintChecks))

}
