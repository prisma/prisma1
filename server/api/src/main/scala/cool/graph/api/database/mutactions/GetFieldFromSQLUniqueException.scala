package cool.graph.api.database.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.api.mutations.CoolArgs
import cool.graph.api.mutations.MutationTypes.ArgumentValue

object GetFieldFromSQLUniqueException {

  def getFieldFromArgumentValueList(values: List[ArgumentValue], e: SQLIntegrityConstraintViolationException): String = {
    values.filter(x => e.getCause.getMessage.contains("\'" + x.name + "_")) match {
      case x if x.nonEmpty => "Field name = " + x.head.name
      case _               => "Sorry, no more details available."
    }
  }

  def getFieldOptionFromArgumentValueList(values: List[ArgumentValue], e: SQLIntegrityConstraintViolationException): Option[String] = {
    values.filter(x => e.getCause.getMessage.contains("\'" + x.name + "_")) match {
      case x if x.nonEmpty => Some("Field name = " + x.head.name)
      case _               => None
    }
  }

  def getFieldFromCoolArgs(values: List[CoolArgs], e: SQLIntegrityConstraintViolationException): String = {
    val combinedValues: List[(String, Any)] = values.flatMap(_.raw)
    combinedValues.filter(x => e.getCause.getMessage.contains("\'" + x._1 + "_")) match {
      case x if x.nonEmpty => "Field name = " + x.head._1
      case _               => "Sorry, no more details available."
    }
  }
}
