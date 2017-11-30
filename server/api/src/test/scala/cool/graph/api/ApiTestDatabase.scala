package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.api.database.DatabaseQueryBuilder.{ResultTransform, _}
import cool.graph.api.database.mutactions.Transaction
import cool.graph.api.database.{DataItem, DataResolver, DatabaseMutationBuilder, DatabaseQueryBuilder}
import cool.graph.shared.database.mutations.{CreateRelationFieldMirrorColumn, CreateRelationTable}
import cool.graph.shared.database.{SqlDDLMutaction}
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.TestProject
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.jdbc.{MySQLProfile, SQLActionBuilder}

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Try

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
    loadProject(project, client)

    // The order here is very important or foreign key constraints will fail
    project.models.foreach(loadModel(project, _))
    project.relations.foreach(loadRelation(project, _))
    project.relations.foreach(loadRelationFieldMirrors(project, _))
  }

  def setupProject(client: Client,
                   project: Project,
                   models: List[Model],
                   relations: List[Relation] = List.empty,
                   rootTokens: List[RootToken] = List.empty): Unit = {
    val actualProject = project.copy(
      models = models,
      relations = relations,
      rootTokens = rootTokens
    )

    setupProject(client, actualProject)
  }

  private def loadProject(project: Project, client: Client): Unit =
    clientDatabase.run(DatabaseMutationBuilder.createClientDatabaseForProject(project.id)).await()

  private def loadModel(project: Project, model: Model): Unit = {
    // For simplicity and for circumventing foreign key constraint violations, load only system fields first
    val plainModel = model.copy(fields = model.fields.filter(_.isSystem))
    clientDatabase.run(DatabaseMutationBuilder.createTableForModel(projectId = project.id, model = model)).await()
  }

  private def loadRelation(project: Project, relation: Relation): Unit = runMutaction(CreateRelationTable(project = project, relation = relation))

  private def loadRelationFieldMirrors(project: Project, relation: Relation): Unit = {
    relation.fieldMirrors.foreach { mirror =>
      runMutaction(CreateRelationFieldMirrorColumn(project, relation, project.getFieldById_!(mirror.fieldId)))
    }
  }

//  def verifyClientMutaction(mutaction: ClientSqlMutaction): Try[MutactionVerificationSuccess] = {
//    val verifyCall = mutaction match {
//      case mutaction: ClientSqlDataChangeMutaction => mutaction.verify(dataResolver)
//      case mutaction                               => mutaction.verify()
//    }
//    verifyCall.await()
//  }

  def runMutaction(mutaction: Transaction): Unit                                = mutaction.execute.await()
  def runDbActionOnClientDb(action: DBIOAction[Any, NoStream, Effect.All]): Any = clientDatabase.run(action).await()

  def runDbActionOnClientDb(pair: (SQLActionBuilder, ResultTransform)): List[DataItem] = {
    val (_, resultTransform) = pair
    val result               = clientDatabase.run(pair._1.as[DataItem]).await().toList
    resultTransform(result).items.toList
  }

  def runMutaction(mutaction: SqlDDLMutaction): Unit = {
    val sqlAction: DBIOAction[Any, NoStream, Effect.All] = mutaction.execute.get
    clientDatabase.run(sqlAction).await()
  }
}
