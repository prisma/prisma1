package com.prisma.api.connector.jdbc.database

import org.jooq.SQLDialect
import slick.jdbc.{JdbcProfile, MySQLProfile, PostgresProfile}

case class Databases(
    primary: SlickDatabase,
    replica: SlickDatabase
)

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
