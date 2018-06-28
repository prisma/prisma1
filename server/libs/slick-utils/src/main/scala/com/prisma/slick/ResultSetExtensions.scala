package com.prisma.slick

import java.sql.{Connection, PreparedStatement, ResultSet}

import scala.collection.mutable

object NewJdbcExtensions {
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
      val result = mutable.Buffer.empty[T]
      while (resultSet.next) {
        result += reads.read(resultSet)
      }
      result.toVector
    }
  }
}
