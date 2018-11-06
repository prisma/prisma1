package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.{SharedJdbcExtensions, SharedSlickExtensions}
import org.jooq.conf.Settings
import org.jooq.impl.DSL

trait JdbcBase extends SharedSlickExtensions with SharedJdbcExtensions {
  lazy val placeHolder = "?"
  lazy val sql         = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))
  lazy val database    = slickDatabase.database
}
