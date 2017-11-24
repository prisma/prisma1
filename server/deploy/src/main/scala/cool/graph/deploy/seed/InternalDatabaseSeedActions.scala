package cool.graph.deploy.seed

import cool.graph.cuid.Cuid
import slick.dbio.{Effect, NoStream}
import slick.jdbc.MySQLProfile.api._

object InternalDatabaseSeedActions {

  /**
    * Returns a sequence of all sql statements that should be run in the current environment.
    */
  def seedActions(): DBIOAction[Vector[Unit], NoStream, Effect] = {
    var actions = Vector.empty[DBIOAction[Unit, NoStream, Effect]]

    actions = actions :+ createMasterConsumerSeedAction()

    DBIO.sequence(actions)
  }

  /**
    * Used to seed the master consumer for local Graphcool setup. Only creates a user if there is no data
    * @return SQL action required to create the master user.
    */
  private def createMasterConsumerSeedAction(): DBIOAction[Unit, NoStream, Effect] = {
    val id = Cuid.createCuid()
    val pw = java.util.UUID.randomUUID().toString

    DBIO.seq(
      sqlu"""
        INSERT INTO Client (id, name, email, gettingStartedStatus, password, createdAt, updatedAt, resetPasswordSecret, source, auth0Id, Auth0IdentityProvider, isAuth0IdentityProviderEmail, isBeta)
        SELECT $id, 'Test', 'test@test.org', '', $pw, NOW(), NOW(), NULL, 'WAIT_LIST', NULL, NULL, 0, 0 FROM DUAL
        WHERE NOT EXISTS (SELECT * FROM Client);
      """
    )
  }
}
