package com.prisma.api.connector.jdbc.extensions

import org.jooq.{Condition, Record, UpdateSetFirstStep}

trait JooqExtensions {
  import JooqExtensionValueClasses._

  implicit def updateSetFirstStepExtensions(x: UpdateSetFirstStep[Record]) = new UpdateSetFirstStepExtensions(x)
  implicit def conditionExtensions(x: Condition)                           = new ConditionExtensions(x)
}

object JooqExtensionValueClasses {
  import org.jooq.impl.DSL._

  import scala.collection.JavaConverters._

  private val placeHolder = "?"

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

  class ConditionExtensions(val x: Condition) extends AnyVal {
    def invert(invert: Boolean): Condition = if (invert) x.not else x
  }
}
