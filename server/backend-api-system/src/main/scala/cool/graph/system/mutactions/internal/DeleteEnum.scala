package cool.graph.system.mutactions.internal

import cool.graph.shared.errors.UserInputErrors.EnumIsReferencedByField
import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.{EnumTable, RelayIdTable}
import cool.graph.shared.models.{Enum, Project}
import cool.graph.{MutactionVerificationSuccess, SystemSqlMutaction, SystemSqlStatementResult}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DeleteEnum(project: Project, enum: Enum) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val enums    = TableQuery[EnumTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful {
      SystemSqlStatementResult {
        DBIO.seq(
          enums.filter(_.id === enum.id).delete,
          relayIds.filter(_.id === enum.id).delete
        )
      }
    }

  }

  override def rollback: Option[Future[SystemSqlStatementResult[Any]]] = {
    Some(CreateEnum(project, enum).execute)
  }

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val referencesToEnum = for {
      model     <- project.models
      field     <- model.fields
      fieldEnum <- field.enum
      if fieldEnum.id == enum.id
    } yield (model.name, field.name)

    val checkIfEnumIsInUse = if (referencesToEnum.nonEmpty) {
      val (modelName, fieldName) = referencesToEnum.head
      Failure(EnumIsReferencedByField(fieldName = fieldName, typeName = modelName))
    } else {
      Success(MutactionVerificationSuccess())
    }

    Future.successful(checkIfEnumIsInUse)
  }
}
