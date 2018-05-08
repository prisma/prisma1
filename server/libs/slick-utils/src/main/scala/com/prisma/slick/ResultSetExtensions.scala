package com.prisma.slick

import java.sql.{Connection, PreparedStatement, ResultSet}

object PreparedStatementExtensions {}

object NewJdbcExtensions {
  // PREPARED STATEMENTS
  trait SetParam[T] {
    def apply(ps: PreparedStatement, index: Int, value: T): Unit
  }

  implicit class PreparedStatementExtensions2(val ps: PreparedStatement) extends AnyVal {
//    def inValues[T](values: Vector[T]) = {
//      //
//    }

    def setValues[T](values: Vector[T])(implicit setParam: SetParam[T]): PreparedStatement = {
      values.zipWithIndex.foreach { valueWithIndex =>
        setParam(ps, valueWithIndex._2 + 1, valueWithIndex._1)
      }
      ps
    }
  }

  // RESULTSETS
  trait ReadsResultSet[T] {
    def read(resultSet: ResultSet): T
  }

  object ReadsResultSet {
    def apply[T](fn: ResultSet => T): ReadsResultSet[T] = new ReadsResultSet[T] {
      override def read(resultSet: ResultSet) = fn(resultSet)
    }
  }

  implicit class ResultSetExtensions2(val resultSet: ResultSet) extends AnyVal {
    def as[T](implicit reads: ReadsResultSet[T]): Vector[T] = {
      var result = Vector.empty[T]
      while (resultSet.next) {
        result = result :+ reads.read(resultSet)
      }
      result
    }
  }

  // CONNECTIONS
  implicit class ConnectionExtensions(val connection: Connection) extends AnyVal {
    //
  }

  // MISC
  def placeHolders(values: Vector[_]): String = "(" + values.map(_ => "?").mkString(",") + ")"
}
