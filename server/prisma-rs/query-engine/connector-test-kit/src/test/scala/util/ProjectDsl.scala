package util

import org.scalatest.Suite

object ProjectDsl {
  val testProjectId = "default@default"

  def fromStringWithId(id: String)(sdlString: String): Project = {
    Project(id = id, dataModel = sdlString)
  }

  def fromString(sdlString: String)(implicit suite: Suite): Project = {
    Project(id = projectId(suite), dataModel = sdlString)
  }

  private def projectId(suite: Suite): String = {
    // GetFieldFromSQLUniqueException blows up if we generate longer names, since we then exceed the postgres limits for constraint names
    // todo: actually fix GetFieldFromSQLUniqueException instead
    val nameThatMightBeTooLong = suite.getClass.getSimpleName
    nameThatMightBeTooLong.substring(0, Math.min(32, nameThatMightBeTooLong.length))
  }
}
