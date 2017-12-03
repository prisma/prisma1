package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.database.{DataResolver, DatabaseMutationBuilder, DatabaseQueryBuilder}
import cool.graph.deploy.migration.mutactions.{ClientSqlMutaction, CreateRelationTable}
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.TestProject
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Await
import scala.concurrent.duration._

trait ApiTestDatabase extends BeforeAndAfterEach with BeforeAndAfterAll with AwaitUtils { self: Suite =>

  implicit lazy val system: ActorSystem             = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()
  implicit lazy val testDependencies                = new ApiDependenciesForTest
  private lazy val databaseManager                  = testDependencies.databaseManager
  lazy val clientDatabase: DatabaseDef              = databaseManager.databases.values.head.master // FIXME: is this ok here?

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    testDependencies.destroy
    materializer.shutdown()
    Await.result(system.terminate(), 5.seconds)
  }

  def dataResolver: DataResolver                   = dataResolver(TestProject())
  def dataResolver(project: Project): DataResolver = new DataResolver(project = project)

  def deleteProjectDatabase(project: Project): Unit = deleteExistingDatabases(Vector(project.id))

  def deleteExistingDatabases: Unit = {
    val schemas = {
      clientDatabase
        .run(DatabaseQueryBuilder.getSchemas)
        .await
        .filter(db => !Vector("information_schema", "mysql", "performance_schema", "sys", "innodb", "graphcool").contains(db))
    }
    deleteExistingDatabases(schemas)
  }

  def deleteExistingDatabases(dbs: Vector[String]): Unit = {
    val dbAction = DBIO.seq(dbs.map(db => DatabaseMutationBuilder.deleteProjectDatabase(projectId = db)): _*)
    clientDatabase.run(dbAction).await(60)
  }

  def truncateProjectDatabase(project: Project): Unit = {
    val tables = clientDatabase.run(DatabaseQueryBuilder.getTables(project.id)).await
    val dbAction = {
      val actions = List(sqlu"""USE `#${project.id}`;""") ++ List(DatabaseMutationBuilder.dangerouslyTruncateTable(tables))
      DBIO.seq(actions: _*)
    }
    clientDatabase.run(dbAction).await()
  }

  def setupProject(client: Client, project: Project, model: Model): Unit = {
    val actualProject = project.copy(models = List(model))
    setupProject(client, actualProject)
  }

  def setupProject(client: Client, project: Project, model: Model, relations: List[Relation]): Unit = {
    val actualProject = project.copy(
      models = List(model),
      relations = relations
    )
    setupProject(client, actualProject)
  }

  def setupProject(client: Client, project: Project): Unit = {
    deleteProjectDatabase(project)
    loadProject(project)

    // The order here is very important or foreign key constraints will fail
    project.models.foreach(loadModel(project, _))
    project.relations.foreach(loadRelation(project, _))
    //project.relations.foreach(loadRelationFieldMirrors(project, _))
  }

  private def loadProject(project: Project): Unit                      = runDbActionOnClientDb(DatabaseMutationBuilder.createClientDatabaseForProject(project.id))
  private def loadModel(project: Project, model: Model): Unit          = runDbActionOnClientDb(DatabaseMutationBuilder.createTableForModel(project.id, model))
  private def loadRelation(project: Project, relation: Relation): Unit = runMutaction(CreateRelationTable(project = project, relation = relation))

//  private def loadRelationFieldMirrors(project: Project, relation: Relation): Unit = {
//    relation.fieldMirrors.foreach { mirror =>
//      runMutaction(CreateRelationFieldMirrorColumn(project, relation, project.getFieldById_!(mirror.fieldId)))
//    }
//  }

  def runMutaction(mutaction: ClientSqlMutaction): Unit                         = runDbActionOnClientDb(mutaction.execute.await().sqlAction)
  def runDbActionOnClientDb(action: DBIOAction[Any, NoStream, Effect.All]): Any = clientDatabase.run(action).await()
}
