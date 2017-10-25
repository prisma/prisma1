package cool.graph

import com.github.tototoshi.slick.MySQLJodaSupport._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.{InternalAndProjectDbs, InternalDatabase}
import cool.graph.shared.models.MutationLogStatus
import cool.graph.shared.models.MutationLogStatus.MutationLogStatus
import cool.graph.system.database.tables.{MutationLog, MutationLogMutaction, Tables}
import cool.graph.utils.future.FutureUtils._
import org.joda.time.DateTime
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Awaitable, Future}
import scala.language.reflectiveCalls

class InternalMutactionRunner(requestContext: Option[SystemRequestContextTrait], databases: InternalAndProjectDbs, logTiming: Function[Timing, Unit]) {
  import InternalMutationMetrics._

  val internalDatabase: InternalDatabase = databases.internal
  lazy val clientDatabase                = databases.client.getOrElse(sys.error("The client database must not be none here")).master
  val internalDatabaseDef                = internalDatabase.databaseDef

  // FIXME: instead of a tuple return an object with proper names
  private def groupMutactions(mutactions: List[(Mutaction, Int)]) =
    mutactions
      .foldLeft(List[(ClientSqlMutaction, Int)](), List[(SystemSqlMutaction, Int)](), List[(Mutaction, Int)]()) {
        case ((xs, ys, zs), x @ (x1: ClientSqlMutaction, _)) =>
          val casted = x.asInstanceOf[(ClientSqlMutaction, Int)]
          (xs :+ casted, ys, zs)

        case ((xs, ys, zs), y @ (y1: SystemSqlMutaction, _)) =>
          val casted = y.asInstanceOf[(SystemSqlMutaction, Int)]
          (xs, ys :+ casted, zs)

        case ((xs, ys, zs), z @ (z1: Mutaction, _)) =>
          (xs, ys, zs :+ z)
      }

  def run(mutation: InternalMutation[_], mutactions: List[Mutaction]): Future[List[MutactionExecutionResult]] = {

    implicit val caseClassFormat: JsonFormats.CaseClassFormat.type = cool.graph.JsonFormats.CaseClassFormat
    import slick.jdbc.MySQLProfile.api._
    import spray.json._

    def defaultHandleErrors: PartialFunction[Throwable, MutactionExecutionResult] = {
      case e: MutactionExecutionResult => e
    }

    def extractTransactionMutactions(mutaction: Mutaction): List[Mutaction] = {
      mutaction match {
        case m: Transaction if m.isInstanceOf[Transaction] => m.clientSqlMutactions
        case m                                             => List(m)
      }
    }

    // make sure index is following execution order
    val mutactionsWithIndex = groupMutactions(mutactions.flatMap(extractTransactionMutactions).map(m => (m, 0))) match {
      case (clientSQLActions, systemSQLActions, otherActions) =>
        (clientSQLActions ++ systemSQLActions ++ otherActions).map(_._1).zipWithIndex
    }

    val (clientSQLActions, systemSQLActions, otherActions) = groupMutactions(mutactionsWithIndex)

    val mutationLog = MutationLog(
      id = Cuid.createCuid(),
      name = mutation.getClass.getSimpleName,
      status = MutationLogStatus.SCHEDULED,
      failedMutaction = None,
      input = mutation.args.toJson.toString,
      startedAt = DateTime.now(),
      finishedAt = None,
      projectId = requestContext.flatMap(_.projectId),
      clientId = requestContext.flatMap(_.client.map(_.id))
    )

    val mutationLogMutactions = mutactionsWithIndex.map {
      case (m, i) =>
        MutationLogMutaction(
          id = Cuid.createCuid(),
          name = m.getClass.getSimpleName,
          index = i,
          status = MutationLogStatus.SCHEDULED,
          input = m.asInstanceOf[Product].toJson.toString,
          finishedAt = None,
          error = None,
          rollbackError = None,
          mutationLogId = mutationLog.id
        )
    }

    def setRollbackStatus(index: Int, status: MutationLogStatus.MutationLogStatus, exception: Option[Throwable]) = {
      implicit val mutationLogStatusMapper: JdbcType[MutationLogStatus] with BaseTypedType[MutationLogStatus] = MutationLog.mutationLogStatusMapper

      val indexed = mutationLogMutactions.find(_.index == index).get

      val mutactionSqlAction = status match {
        case MutationLogStatus.ROLLEDBACK =>
          List((for { l <- Tables.MutationLogMutactions if l.id === indexed.id } yield l.status).update(status))

        case MutationLogStatus.FAILURE =>
          List((for { l <- Tables.MutationLogMutactions if l.id === indexed.id } yield l.rollbackError).update(exception.map(formatException)))

        case _ => List()
      }

      val mutationSqlAction = (index, status) match {
        case (0, MutationLogStatus.ROLLEDBACK) =>
          List((for { m <- Tables.MutationLogs if m.id === mutationLog.id } yield (m.status, m.finishedAt)).update((status, Some(DateTime.now()))))

        case _ =>
          List()
      }

      DBIO.seq(mutactionSqlAction ++ mutationSqlAction: _*).transactionally
    }

    def formatException(exception: Throwable) =
      s"${exception.getMessage} \n\n${exception.toString} \n\n${exception.getStackTrace
        .map(_.toString)
        .mkString(" \n")}"

    def setStatus(index: Int, status: MutationLogStatus.MutationLogStatus, exception: Option[Throwable]) = {
      implicit val mutationLogStatusMapper: JdbcType[MutationLogStatus] with BaseTypedType[MutationLogStatus] = MutationLog.mutationLogStatusMapper

      val indexed = mutationLogMutactions.find(_.index == index).get

      val q = for { l <- Tables.MutationLogMutactions if l.id === indexed.id } yield (l.status, l.finishedAt, l.error)

      val mutactionSqlAction = List(q.update((status, Some(DateTime.now()), exception.map(formatException))))

      val lastIndex = mutationLogMutactions.map(_.index).max

      val mutationSqlAction = (index, status) match {
        case (lastIndex, MutationLogStatus.SUCCESS) =>
          // STATUS, finishedAt
          List((for { m <- Tables.MutationLogs if m.id === mutationLog.id } yield (m.status, m.finishedAt)).update((status, Some(DateTime.now()))))

        case (0, MutationLogStatus.FAILURE) =>
          // FAILURE. No rollback needed
          List(
            (for { m <- Tables.MutationLogs if m.id === mutationLog.id } yield
              (m.status, m.failedMutaction)).update((MutationLogStatus.ROLLEDBACK, Some(indexed.name))))

        case (_, MutationLogStatus.FAILURE) =>
          // FAILURE. Begin rollback
          List((for { m <- Tables.MutationLogs if m.id === mutationLog.id } yield (m.status, m.failedMutaction)).update((status, Some(indexed.name))))

        case _ =>
          // noop
          List()
      }

      DBIO.seq(mutactionSqlAction ++ mutationSqlAction: _*)
    }

    def logAndRollback[A](index: Int, f: Future[A]): Future[A] = {
      f.andThenFuture(
        handleSuccess = _ => internalDatabaseDef.run(setStatus(index, MutationLogStatus.SUCCESS, None)),
        handleFailure = e => {
          internalDatabaseDef
            .run(setStatus(index, MutationLogStatus.FAILURE, Some(e)))
            .flatMap(_ => {
              val rollbackFutures = mutactionsWithIndex
                .takeWhile(_._2 < index)
                .reverse
                .map(m => {
                  // rollback and log
                  val rollbackFuture = m._1 match {
                    case mutaction: SystemSqlMutaction =>
                      mutaction.rollback match {
                        case None           => Future.failed(new Exception(s"Rollback not implemented: ${mutaction.getClass.getSimpleName}"))
                        case Some(rollback) => internalDatabaseDef.run(await(rollback).sqlAction)
                      }

                    case mutaction: ClientSqlMutaction =>
                      mutaction.rollback match {
                        case None           => Future.failed(new Exception(s"Rollback not implemented: ${mutaction.getClass.getSimpleName}"))
                        case Some(rollback) => clientDatabase.run(await(rollback).sqlAction)
                      }

                    case mutaction =>
                      Future.successful(()) // only rolling back sql mutactions
                  }

                  rollbackFuture
                    .andThenFuture(
                      handleSuccess = _ => internalDatabaseDef.run(setRollbackStatus(m._2, MutationLogStatus.ROLLEDBACK, None)),
                      handleFailure = e => internalDatabaseDef.run(setRollbackStatus(m._2, MutationLogStatus.FAILURE, Some(e)))
                    )
                })

              // Todo: this is absolutely useless, Futures are already running in parallel. Massive bug that just happens to work by chance.
              rollbackFutures.map(() => _).runSequentially
            })
        }
      )
    }

    def createLogFuture =
      mutationLogMutactions.length match {
        case 0 => Future.successful(())
        case _ =>
          internalDatabaseDef.run(
            DBIO.seq(List(Tables.MutationLogs += mutationLog) ++
              mutationLogMutactions.map(m => Tables.MutationLogMutactions += m): _*))
      }

    // todo: make this run in transaction
    // todo decide how to handle execution results from runOnClientDatabase
    // todo: when updating both internal and client database - how should we handle failures?
    def clientSqlActionsResultFuture: Future[List[MutactionExecutionResult]] =
      clientSQLActions.map { action => () =>
        def executeAction = mutactionTimer.timeFuture(customTagValues = action.getClass.getSimpleName) {
          clientDatabase.run(await(action._1.execute).sqlAction)
        }

        logAndRollback(
          action._2,
          InternalMutation.performWithTiming(
            s"execute ${action.getClass.getSimpleName}",
            performWithTiming("clientSqlAction", executeAction),
            logTiming
          )
        ).map(_ => MutactionExecutionSuccess())
          .recover(
            action._1.handleErrors
              .getOrElse(defaultHandleErrors)
              .andThen({ case e: Throwable => throw e }))
      }.runSequentially

    def systemSqlActionsResultFuture: Future[List[MutactionExecutionResult]] =
      systemSQLActions.map { action => () =>
        def executeAction = mutactionTimer.timeFuture(customTagValues = action.getClass.getSimpleName) {
          internalDatabaseDef.run(
            await(InternalMutation.performWithTiming(s"execute ${action.getClass.getSimpleName}", action._1.execute, logTiming)).sqlAction)
        }

        logAndRollback(
          action._2,
          executeAction
        ).map(_ => MutactionExecutionSuccess())
          .recover(
            action._1.handleErrors
              .getOrElse(defaultHandleErrors)
              .andThen({ case e: Throwable => throw e }))
      }.runSequentially

    def otherExecutionResultFuture: Future[List[MutactionExecutionResult]] =
      otherActions.map { action => () =>
        def executeAction = mutactionTimer.timeFuture(customTagValues = action.getClass.getSimpleName) {
          action._1.execute
        }
        logAndRollback(action._2,
                       InternalMutation
                         .performWithTiming(s"execute ${action.getClass.getSimpleName}", executeAction, logTiming))
          .recover(
            action._1.handleErrors
              .getOrElse(defaultHandleErrors)
              .andThen({ case e: Throwable => throw e }))
      }.runSequentially

    for {
      createLogResult        <- createLogFuture
      clientSqlActionsResult <- clientSqlActionsResultFuture
      systemSqlActionsResult <- systemSqlActionsResultFuture
      otherExecutionResult   <- otherExecutionResultFuture
    } yield {
      clientSqlActionsResult ++ systemSqlActionsResult ++ otherExecutionResult
    }
  }

  private def await[T](awaitable: Awaitable[T]): T = {
    import scala.concurrent.duration._
    Await.result(awaitable, 15.seconds)
  }

  private def performWithTiming[A](name: String, f: Future[A]): Future[A] = {
    val begin = System.currentTimeMillis()
    f andThen {
      case x =>
        requestContext.foreach(_.logSqlTiming(Timing(name, System.currentTimeMillis() - begin)))
        x
    }
  }
}
