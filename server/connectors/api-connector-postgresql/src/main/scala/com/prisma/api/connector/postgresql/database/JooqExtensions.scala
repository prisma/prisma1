package com.prisma.api.connector.postgresql.database

import org.jooq.{Record, UpdateSetFirstStep}

object JooqExtensions {
  import org.jooq.impl.DSL._
  import scala.collection.JavaConverters._

  import JooqQueryBuilders._

  implicit class UpdateSetFirstStepExtensions(val x: UpdateSetFirstStep[Record]) extends AnyVal {
    def setColumnsWithPlaceHolders(columns: scala.collection.immutable.Seq[String]) = {
      require(columns.nonEmpty)

      // this blows up if we use the row syntax (col1, col2, ...) if there is only one column
      if (columns.size > 1) {
        val values = columns.map(_ => placeHolder)
        val fields = columns.map(c => field(name(c)))

        x.set(row(fields.asJava), row(values.asJava))
      } else {
        x.set(field(columns.head), placeHolder)
      }
    }
  }
}
