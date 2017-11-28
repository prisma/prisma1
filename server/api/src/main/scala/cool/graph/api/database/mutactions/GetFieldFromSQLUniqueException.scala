package cool.graph.api.database.mutactions

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.api.mutations.MutationTypes.ArgumentValue

object GetFieldFromSQLUniqueException {

  def getField(values: List[ArgumentValue], e: SQLIntegrityConstraintViolationException): String = {
    values.filter(x => e.getCause.getMessage.contains("\'" + x.name + "_")) match {
      case x if x.nonEmpty => "Field name = " + x.head.name
      case _               => "Sorry, no more details available."
    }
  }
}
