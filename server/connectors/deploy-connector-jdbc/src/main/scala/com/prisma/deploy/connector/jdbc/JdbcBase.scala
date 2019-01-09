package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.{SharedJdbcExtensions, SharedSlickExtensions}
import org.jooq.conf.Settings
import org.jooq.impl.DSL

trait JdbcBase extends SharedSlickExtensions with SharedJdbcExtensions {
  lazy val placeHolder = "?"
  lazy val database    = slickDatabase.database

  implicit lazy val sql = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))

  // Dialect specific name qualification and escaping. Use this for direct interpolations, for example into slick sql""
  def qualify(n: String*): String = sql.render(DSL.name(n: _*))
}
