package cool.graph.deploy.seed

import slick.dbio.{Effect, NoStream}
import slick.jdbc.MySQLProfile.api._

object InternalDatabaseSeedActions {

  /**
    * Returns a sequence of all sql statements that should be run in the current environment.
    */
  def seedActions(): DBIOAction[Vector[Unit], NoStream, Effect] = {
    var actions = Vector.empty[DBIOAction[Unit, NoStream, Effect]]
    DBIO.sequence(actions)
  }
}
