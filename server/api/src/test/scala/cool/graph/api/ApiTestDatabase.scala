package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.database.{DatabaseMutationBuilder, DatabaseQueryBuilder}
import cool.graph.deploy.migration.mutactions.{ClientSqlMutaction, CreateRelationTable}
import cool.graph.shared.models._
import cool.graph.utils.await.AwaitUtils
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

case class ApiTestDatabase()(implicit dependencies: ApiDependencies) extends AwaitUtils {

  implicit lazy val system: ActorSystem             = dependencies.system
  implicit lazy val materializer: ActorMaterializer = dependencies.materializer
  private lazy val clientDatabase: DatabaseDef      = dependencies.databases.master

  def setup(project: Project): Unit = {
    delete(project)
    createProjectDatabase(project)

    // The order here is very important or foreign key constraints will fail
    project.models.foreach(createModelTable(project, _))
    project.relations.foreach(createRelationTable(project, _))
  }

  def truncate(project: Project): Unit = {
    val tables = clientDatabase.run(DatabaseQueryBuilder.getTables(project.id)).await
    val dbAction = {
      val actions = List(sqlu"""USE `#${project.id}`;""") ++ List(DatabaseMutationBuilder.dangerouslyTruncateTable(tables))
      DBIO.seq(actions: _*)
    }
    clientDatabase.run(dbAction).await()
  }

  def delete(project: Project): Unit = dropDatabases(Vector(project.id))

  private def createProjectDatabase(project: Project): Unit                   = runDbActionOnClientDb(DatabaseMutationBuilder.createClientDatabaseForProject(project.id))
  private def createModelTable(project: Project, model: Model): Unit          = runDbActionOnClientDb(DatabaseMutationBuilder.createTableForModel(project.id, model))
  private def createRelationTable(project: Project, relation: Relation): Unit = runMutaction(CreateRelationTable(project = project, relation = relation))

  //  def loadRelationFieldMirrors(project: Project, relation: Relation): Unit = {
//    relation.fieldMirrors.foreach { mirror =>
//      runMutaction(CreateRelationFieldMirrorColumn(project, relation, project.getFieldById_!(mirror.fieldId)))
//    }
//  }

  def deleteExistingDatabases(): Unit = {
    val schemas = {
      clientDatabase
        .run(DatabaseQueryBuilder.getSchemas)
        .await
        .filter(db => !Vector("information_schema", "mysql", "performance_schema", "sys", "innodb", "graphcool").contains(db))
    }
    dropDatabases(schemas)
  }

  private def dropDatabases(dbs: Vector[String]): Unit = {
    val dbAction = DBIO.seq(dbs.map(db => DatabaseMutationBuilder.dropDatabaseIfExists(database = db)): _*)
    clientDatabase.run(dbAction).await(60)
  }

  private def runMutaction(mutaction: ClientSqlMutaction): Unit                 = runDbActionOnClientDb(mutaction.execute.await().sqlAction)
  def runDbActionOnClientDb(action: DBIOAction[Any, NoStream, Effect.All]): Any = clientDatabase.run(action).await()
}
