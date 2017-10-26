package cool.graph.system.database.seed

import cool.graph.cuid.Cuid
import slick.dbio.{Effect, NoStream}
import slick.jdbc.MySQLProfile.api._

object InternalDatabaseSeedActions {

  /**
    * Returns a sequence of all sql statements that should be run in the current environment.
    */
  def seedActions(masterToken: Option[String]): DBIOAction[Vector[Unit], NoStream, Effect] = {
    var actions = Vector.empty[DBIOAction[Unit, NoStream, Effect]]

    if (masterToken.isDefined) {
      actions = actions :+ createMasterConsumerSeedAction()
      actions = actions :+ createProjectDatabaseSeedAction()
    }

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

  /**
    * Used to seed the basic ProjectDatabase for local Graphcool setup.
    * @return SQL action required to create the ProjectDatabase.
    */
  private def createProjectDatabaseSeedAction(): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(
      sqlu"""
        INSERT INTO ProjectDatabase (id, region, name, isDefaultForRegion)
        SELECT 'eu-west-1-client-1', 'eu-west-1', 'client1', 1 FROM DUAL
        WHERE NOT EXISTS (SELECT * FROM ProjectDatabase);
      """
    )
  }
}
