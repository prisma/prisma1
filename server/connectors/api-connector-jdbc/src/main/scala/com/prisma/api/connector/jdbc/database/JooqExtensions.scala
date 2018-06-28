package com.prisma.api.connector.jdbc.database

import org.jooq.{Record, UpdateSetFirstStep}

trait JooqExtensions {
  import JooqExtensionValueClasses._

  implicit def updateSetFirstStepExtensions(x: UpdateSetFirstStep[Record]) = new UpdateSetFirstStepExtensions(x)
}

object JooqExtensionValueClasses {
  import scala.collection.JavaConverters._
  import org.jooq.impl.DSL._
  import JooqQueryBuilders.placeHolder

  class UpdateSetFirstStepExtensions(val x: UpdateSetFirstStep[Record]) extends AnyVal {
    def setColumnsWithPlaceHolders(columns: scala.collection.immutable.Seq[String]) = {
      require(columns.nonEmpty)

      // this blows up if we use the row syntax (col1, col2, ...) when there is only one column
      if (columns.size > 1) {
        val values = columns.map(_ => placeHolder)
        val fields = columns.map(c => field(name(c)))

        x.set(row(fields.asJava), row(values.asJava))
      } else {
        x.set(field(name(columns.head)), placeHolder)
      }
    }
  }
}
