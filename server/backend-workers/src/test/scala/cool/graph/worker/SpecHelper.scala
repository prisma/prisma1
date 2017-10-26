package cool.graph.worker

import scala.concurrent.Await

object SpecHelper {
  import slick.jdbc.MySQLProfile.api._

  import scala.concurrent.duration._

  def recreateLogSchemaActions(): DBIOAction[Unit, NoStream, Effect] = DBIO.seq(dropAction, setupActions)

  lazy val dropAction = DBIO.seq(sqlu"DROP SCHEMA IF EXISTS `logs`;")

  lazy val setupActions = DBIO.seq(
    sqlu"CREATE SCHEMA IF NOT EXISTS `logs` DEFAULT CHARACTER SET utf8mb4;",
    sqlu"USE `logs`;",
    sqlu"""
    CREATE TABLE IF NOT EXISTS `Log` (
      `id` varchar(25) NOT NULL,
      `projectId` varchar(25) NOT NULL,
      `functionId` varchar(25) NOT NULL,
      `requestId` varchar(25) NOT NULL,
      `status` enum('SUCCESS','FAILURE') NOT NULL,
      `duration` int(11) NOT NULL,
      `timestamp` datetime(3) NOT NULL,
      `message` mediumtext NOT NULL,
      PRIMARY KEY (`id`),
      KEY `functionId` (`functionId`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"""
  )

  def recreateLogsDatabase(): Unit = {
    val logsRoot = Database.forConfig("logsRoot")
    Await.result(logsRoot.run(SpecHelper.recreateLogSchemaActions()), 30.seconds)
    logsRoot.close()
  }
}
