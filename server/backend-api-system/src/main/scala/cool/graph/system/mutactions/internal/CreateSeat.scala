package cool.graph.system.mutactions.internal

import cool.graph.shared.errors.UserInputErrors.CollaboratorProjectWithNameAlreadyExists
import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.externalServices.SnsPublisher
import cool.graph.system.database.tables.{ProjectTable, RelayIdTable, SeatTable}
import cool.graph.shared.models._
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery
import spray.json.{JsObject, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateSeat(client: Client, project: Project, seat: Seat, internalDatabase: DatabaseDef, ignoreDuplicateNameVerificationError: Boolean = false)(
    implicit inj: Injector)
    extends SystemSqlMutaction
    with Injectable {

  val seatSnsPublisher: SnsPublisher = inject[SnsPublisher](identified by "seatSnsPublisher")

  if (!seat.clientId.contains(project.ownerId)) {
    seatSnsPublisher.putRecord(
      JsObject(
        "action"      -> JsString("ADD"),
        "projectId"   -> JsString(project.id),
        "projectName" -> JsString(project.name),
        "email"       -> JsString(seat.email),
        "status"      -> JsString(seat.status.toString),
        "byEmail"     -> JsString(client.email),
        "byName"      -> JsString(client.name)
      ).compactPrint)
  }

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val seats    = TableQuery[SeatTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO
          .seq(
            seats += cool.graph.system.database.tables
              .Seat(id = seat.id, status = seat.status, email = seat.email, clientId = seat.clientId, projectId = project.id),
            relayIds +=
              cool.graph.system.database.tables.RelayId(seat.id, "Seat")
          )
      ))
  }

  override def rollback = Some(DeleteSeat(client, project, seat, internalDatabase).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {

    seat.clientId match {
      case None =>
        // pending collaborators do not have projects yet.
        Future.successful(Success(MutactionVerificationSuccess()))

      case Some(id) =>
        ignoreDuplicateNameVerificationError match {
          case true =>
            Future.successful(Success(MutactionVerificationSuccess()))

          case false =>
            val projects = TableQuery[ProjectTable]
            internalDatabase
              .run(projects.filter(p => p.clientId === id && p.name === project.name).length.result)
              .map {
                case 0 => Success(MutactionVerificationSuccess())
                case _ => Failure(CollaboratorProjectWithNameAlreadyExists(name = project.name))
              }
        }
    }
  }
}
