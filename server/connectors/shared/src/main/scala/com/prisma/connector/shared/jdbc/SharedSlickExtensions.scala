package com.prisma.connector.shared.jdbc

import java.sql.{PreparedStatement, ResultSet, Statement}

import org.jooq.{Query => JooqQuery, _}
import org.slf4j.LoggerFactory
import slick.jdbc.PositionedParameters

trait SharedSlickExtensions {
  val slickDatabase: SlickDatabase
  import slickDatabase.profile.api._

  val logger = LoggerFactory.getLogger("prisma")

  def queryToDBIO[T](query: JooqQuery)(setParams: PositionedParameters => Unit = (_) => (), readResult: ResultSet => T): DBIO[T] = {
    logger.trace(s"Executing query: ${query.getSQL}")
    jooqToDBIO(query, setParams) { ps =>
      val rs = ps.executeQuery()
      readResult(rs)
    }
  }

  def changeDatabaseQueryToDBIO(query: JooqQuery)(setParams: PositionedParameters => Unit = (_) => ()): DBIO[Unit] = {
    jooqToDBIO(query, setParams) { ps =>
      ps.execute()
    }
  }

  def insertReturningGeneratedKeysToDBIO[T](query: Insert[Record])(setParams: PositionedParameters => Unit = (_) => (), readResult: ResultSet => T): DBIO[T] = {
    jooqToDBIO(query, setParams, returnGeneratedKeys = true)(
      statementFn = { ps =>
        ps.execute()
        readResult(ps.getGeneratedKeys)
      }
    )
  }

  def insertToDBIO[T](query: Insert[Record])(setParams: PositionedParameters => Unit = (_) => ()): DBIO[Unit] = jooqToDBIO(query, setParams)(_.execute())
  def deleteToDBIO(query: Delete[Record])(setParams: PositionedParameters => Unit = (_) => ()): DBIO[Unit]    = jooqToDBIO(query, setParams)(_.execute())
  def updateToDBIO(query: Update[Record])(setParams: PositionedParameters => Unit = (_) => ()): DBIO[Int]     = jooqToDBIO(query, setParams)(_.executeUpdate)
  def truncateToDBIO(query: Truncate[Record]): DBIO[Unit]                                                     = jooqToDBIO(query, _ => ())(_.executeUpdate())

  private def jooqToDBIO[T](
      query: JooqQuery,
      setParams: PositionedParameters => Unit,
      returnGeneratedKeys: Boolean = false
  )(statementFn: PreparedStatement => T): DBIO[T] = {
    SimpleDBIO { ctx =>
      val ps = if (returnGeneratedKeys) {
        ctx.connection.prepareStatement(query.getSQL, Statement.RETURN_GENERATED_KEYS)
      } else {
        ctx.connection.prepareStatement(query.getSQL)
      }

      val pp = new PositionedParameters(ps)
      setParams(pp)
      statementFn(ps)
    }
  }
}
