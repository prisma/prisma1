package com.prisma.connector.shared.jdbc

import org.jooq.SQLDialect
import slick.jdbc.{JdbcProfile, MySQLProfile, PostgresProfile}

import scala.concurrent.{ExecutionContext, Future}

case class Databases(
    primary: SlickDatabase,
    replica: SlickDatabase
) {
  def shutdown(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- primary.database.shutdown
      _ <- replica.database.shutdown
    } yield ()
  }
}

case class SlickDatabase(
    profile: JdbcProfile,
    database: JdbcProfile#Backend#Database
) {
  val dialect: SQLDialect = profile match {
    case PostgresProfile => SQLDialect.POSTGRES_9_5
    case MySQLProfile    => SQLDialect.MYSQL_5_7
    case x               => sys.error(s"No Jooq SQLDialect for Slick profile $x configured yet")
  }

  val isMySql    = dialect.family() == SQLDialect.MYSQL
  val isPostgres = dialect.family() == SQLDialect.POSTGRES
}
