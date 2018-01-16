package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.deploy.database.DatabaseMutationBuilder
import cool.graph.deploy.migration.mutactions.ClientSqlMutaction
import cool.graph.shared.models.Project
import cool.graph.utils.await.AwaitUtils
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

class ClientTestDatabase()(implicit system: ActorSystem, materializer: ActorMaterializer) extends AwaitUtils {
  lazy val clientDatabase = Database.forConfig("client")

//  def setup(project: Project): Unit = {
//    delete(project)
//    createProjectDatabase(project)
//
//    // The order here is very important or foreign key constraints will fail
//    project.models.foreach(createModelTable(project, _))
//    project.relations.foreach(createRelationTable(project, _))
//  }

//  def truncate(project: Project): Unit = {
//    val tables = clientDatabase.run(DatabaseQueryBuilder.getTables(project.id)).await
//    val dbAction = {
//      val actions = List(sqlu"""USE `#${project.id}`;""") ++ dangerouslyTruncateTables(tables)
//      DBIO.seq(actions: _*)
//    }
//
//    clientDatabase.run(dbAction).await()
//  }

//  private def dangerouslyTruncateTables(tableNames: Vector[String]): List[SqlAction[Int, NoStream, Effect]] = {
//      List(sqlu"""SET FOREIGN_KEY_CHECKS=0""") ++
//        tableNames.map(name => sqlu"TRUNCATE TABLE `#$name`") ++
//        List(sqlu"""SET FOREIGN_KEY_CHECKS=1"""): _*
//  }

  def delete(projectId: String): Unit = dropDatabases(Vector(projectId))

//  private def createProjectDatabase(project: Project): Unit                   = runDbActionOnClientDb(DatabaseMutationBuilder.createClientDatabaseForProject(project.id))
//  private def createModelTable(project: Project, model: Model): Unit          = runDbActionOnClientDb(DatabaseMutationBuilder.createTableForModel(project.id, model))
//  private def createRelationTable(project: Project, relation: Relation): Unit = runMutaction(CreateRelationTable(project = project, relation = relation))

  //  def loadRelationFieldMirrors(project: Project, relation: Relation): Unit = {
  //    relation.fieldMirrors.foreach { mirror =>
  //      runMutaction(CreateRelationFieldMirrorColumn(project, relation, project.getFieldById_!(mirror.fieldId)))
  //    }
  //  }

//  def deleteExistingDatabases(): Unit = {
//    val schemas = {
//      clientDatabase
//        .run(DatabaseQueryBuilder.getSchemas)
//        .await
//        .filter(db => !Vector("information_schema", "mysql", "performance_schema", "sys", "innodb", "graphcool").contains(db))
//    }
//    dropDatabases(schemas)
//  }

  private def dropDatabases(dbs: Vector[String]): Unit = {
    val dbAction = DBIO.seq(dbs.map(db => DatabaseMutationBuilder.dropClientDatabaseForProject(db)): _*)
    clientDatabase.run(dbAction).await(60)
  }

  private def runMutaction(mutaction: ClientSqlMutaction): Unit                         = runDbActionOnClientDb(mutaction.execute.await().sqlAction)
  private def runDbActionOnClientDb(action: DBIOAction[Any, NoStream, Effect.All]): Any = clientDatabase.run(action).await()

  def shutdown() = {
    clientDatabase.close()
  }
}
