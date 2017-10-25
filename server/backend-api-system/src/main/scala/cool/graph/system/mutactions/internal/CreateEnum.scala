package cool.graph.system.mutactions.internal

import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.{EnumTable, RelayIdTable}
import cool.graph.shared.models.{Enum, Project}
import cool.graph.system.mutactions.internal.validations.{EnumValueValidation, TypeNameValidation}
import cool.graph.{MutactionVerificationSuccess, SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future
import scala.util.Try

case class CreateEnum(project: Project, enum: Enum) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val enums    = TableQuery[EnumTable]
    val relayIds = TableQuery[RelayIdTable]
    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          enums += cool.graph.system.database.tables.Enum(enum.id, project.id, enum.name, enum.values.toJson.compactPrint),
          relayIds += cool.graph.system.database.tables.RelayId(enum.id, enums.baseTableRow.tableName)
        )
      }
    }
  }

  override def rollback: Some[Future[SystemSqlStatementResult[Any]]] = Some(DeleteEnum(project, enum).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    Future.successful {
      for {
        _ <- TypeNameValidation.validateEnumName(project, enum.name)
        _ <- EnumValueValidation.validateEnumValues(enum.values)
      } yield MutactionVerificationSuccess()
    }
  }
}
