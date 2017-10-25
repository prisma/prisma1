package cool.graph.system.mutactions.internal

import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.EnumTable
import cool.graph.shared.models.Enum
import cool.graph.system.mutactions.internal.validations.EnumValueValidation
import cool.graph.{MutactionVerificationSuccess, SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future
import scala.util.Try

case class UpdateEnum(newEnum: Enum, oldEnum: Enum) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val enums = TableQuery[EnumTable]
    val query = for {
      enum <- enums
      if enum.id === oldEnum.id
    } yield (enum.name, enum.values)

    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          query.update(newEnum.name, newEnum.values.toJson.compactPrint)
        )
      }
    }
  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = Some(UpdateEnum(newEnum = oldEnum, oldEnum = oldEnum).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    Future.successful {
      for {
        _ <- EnumValueValidation.validateEnumValues(newEnum.values)
      } yield MutactionVerificationSuccess()
    }
  }
}
