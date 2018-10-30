package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.{SharedJdbcExtensions, SharedSlickExtensions}

trait JdbcPersistenceBase extends SharedSlickExtensions with SharedJdbcExtensions {
  val placeHolder           = "?"
  def value(v: Any): AnyRef = v.asInstanceOf[AnyRef] // does the implicit conversions required for Jooq
}
