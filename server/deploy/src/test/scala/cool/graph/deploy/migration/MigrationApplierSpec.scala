package cool.graph.deploy.migration

import cool.graph.deploy.migration.migrator.MigrationApplierImpl
import cool.graph.deploy.migration.mutactions.{ClientSqlMutaction, ClientSqlStatementResult}
import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models._
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import slick.dbio.DBIOAction

import scala.concurrent.Future

class MigrationApplierSpec extends FlatSpec with Matchers with DeploySpecBase with AwaitUtils {
  import system.dispatcher
  val persistence = testDependencies.migrationPersistence

  val projectId   = "test-project-id"
  val emptySchema = Schema()
  val migration = Migration(
    projectId = projectId,
    revision = 1,
    schema = emptySchema,
    functions = Vector.empty,
    status = MigrationStatus.Pending,
    applied = 0,
    rolledBack = 0,
    steps = Vector(CreateModel("Step1"), CreateModel("Step2"), CreateModel("Step3")),
    errors = Vector.empty
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    testDependencies.projectPersistence.create(Project(projectId, "ownerId", 1, emptySchema)).await
  }

  "the applier" should "succeed when all steps succeed" in {
    persistence.create(migration).await
    val mapper  = stepMapper { case _ => succeedingSqlMutactionWithSucceedingRollback }
    val applier = migrationApplier(mapper)
    val result  = applier.apply(previousSchema = emptySchema, migration = migration).await
    result.succeeded should be(true)

    val persisted = persistence.getLastMigration(projectId).await.get
    persisted.status should be(MigrationStatus.Success)
    persisted.applied should be(migration.steps.size)
    persisted.rolledBack should be(0)
  }

  "the applier" should "mark a migration as ROLLBACK_SUCCESS if all steps can be rolled back successfully" in {
    persistence.create(migration).await

    val mapper = stepMapper({
      case CreateModel("Step1") => succeedingSqlMutactionWithSucceedingRollback
      case CreateModel("Step2") => failingSqlMutactionWithSucceedingRollback
      case CreateModel("Step3") => succeedingSqlMutactionWithSucceedingRollback
    })

    val applier = migrationApplier(mapper)
    val result  = applier.apply(previousSchema = emptySchema, migration = migration).await
    result.succeeded should be(false)

    val persisted = loadMigrationFromDb
    persisted.status should be(MigrationStatus.RollbackSuccess)
    persisted.applied should be(1)    // 1 step succeeded
    persisted.rolledBack should be(1) // 1 step was rolled back
  }

  "the applier" should "mark a migration as ROLLBACK_FAILURE if the rollback fails" in {
    persistence.create(migration).await

    val mapper = stepMapper({
      case CreateModel("Step1") => succeedingSqlMutactionWithFailingRollback
      case CreateModel("Step2") => succeedingSqlMutactionWithSucceedingRollback
      case CreateModel("Step3") => failingSqlMutactionWithSucceedingRollback
    })

    val applier = migrationApplier(mapper)
    val result  = applier.apply(previousSchema = emptySchema, migration = migration).await
    result.succeeded should be(false)

    val persisted = loadMigrationFromDb
    persisted.status should be(MigrationStatus.RollbackFailure)
    persisted.applied should be(2)    // 2 steps succeeded
    persisted.rolledBack should be(1) // 1 steps were rolled back
  }

  def loadMigrationFromDb: Migration = persistence.byId(migration.id).await.get

  def migrationApplier(stepMapper: MigrationStepMapper) = MigrationApplierImpl(persistence, clientDb.clientDatabase, stepMapper)

  lazy val succeedingSqlMutactionWithSucceedingRollback = clientSqlMutaction(succeedingStatementResult, rollback = succeedingStatementResult)
  lazy val succeedingSqlMutactionWithFailingRollback    = clientSqlMutaction(succeedingStatementResult, rollback = failingStatementResult)
  lazy val failingSqlMutactionWithSucceedingRollback    = clientSqlMutaction(failingStatementResult, rollback = succeedingStatementResult)
  lazy val failingSqlMutactionWithFailingRollback       = clientSqlMutaction(failingStatementResult, rollback = failingStatementResult)
  lazy val succeedingStatementResult                    = ClientSqlStatementResult[Any](DBIOAction.successful(()))
  lazy val failingStatementResult                       = ClientSqlStatementResult[Any](DBIOAction.failed(new Exception("failing statement result")))

  def clientSqlMutaction(execute: ClientSqlStatementResult[Any], rollback: ClientSqlStatementResult[Any]): ClientSqlMutaction = {
    clientSqlMutaction(execute, Some(rollback))
  }

  def clientSqlMutaction(execute: ClientSqlStatementResult[Any], rollback: Option[ClientSqlStatementResult[Any]] = None): ClientSqlMutaction = {
    val (executeArg, rollbackArg) = (execute, rollback)
    new ClientSqlMutaction {
      override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(executeArg)

      override def rollback: Option[Future[ClientSqlStatementResult[Any]]] = rollbackArg.map(Future.successful)
    }
  }

  def stepMapper(pf: PartialFunction[MigrationStep, ClientSqlMutaction]) = new MigrationStepMapper {
    override def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Option[ClientSqlMutaction] = {
      pf.lift.apply(step)
    }
  }
}
