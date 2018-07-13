package com.prisma.slick

import java.sql.ResultSet

import com.prisma.slick.ResultSetExtensionsValueClasses.ResultSetExtensions2

import scala.collection.mutable
import scala.language.implicitConversions

trait ReadsResultSet[T] {
  def read(resultSet: ResultSet): T
}

object ReadsResultSet {
  def apply[T](fn: ResultSet => T): ReadsResultSet[T] = new ReadsResultSet[T] {
    override def read(resultSet: ResultSet) = fn(resultSet)
  }
}

object ResultSetExtensions extends ResultSetExtensions
trait ResultSetExtensions {
  implicit def resultSetExtensions2(resultSet: ResultSet) = new ResultSetExtensions2(resultSet)
}

object ResultSetExtensionsValueClasses {
  class ResultSetExtensions2(val resultSet: ResultSet) extends AnyVal {

    def readWith[T](reads: ReadsResultSet[T]): Vector[T] = {
      val result = mutable.Buffer.empty[T]
      while (resultSet.next) {
        result += reads.read(resultSet)
      }
      result.toVector
    }
  }
}
