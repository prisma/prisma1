package com.prisma.deploy.migration.migrator

import com.prisma.IgnoreSQLite
import com.prisma.deploy.connector._
import com.prisma.deploy.specutils.{ActiveDeploySpecBase, TestProject}
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

class MigrationApplierSpec extends FlatSpec with Matchers with ActiveDeploySpecBase with AwaitUtils {
  import system.dispatcher
  val persistence = testDependencies.migrationPersistence

  val project     = TestProject()
  val projectId   = project.id
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
    errors = Vector.empty,
    previousSchema = Schema.empty,
    rawDataModel = ""
  )

  val step1Model = Model.empty.copy(name = "Step1").build(emptySchema)
  val step2Model = Model.empty.copy(name = "Step2").build(emptySchema)
  val step3Model = Model.empty.copy(name = "Step3").build(emptySchema)

  val mapper = stepMapper({
    case CreateModel("Step1") => Vector(CreateModelTable(project, step1Model))
    case CreateModel("Step2") => Vector(CreateModelTable(project, step2Model))
    case CreateModel("Step3") => Vector(CreateModelTable(project, step3Model))
  })

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    testDependencies.projectPersistence.create(Project(projectId, 1, emptySchema)).await
  }

  "the applier" should "succeed when all steps succeed" taggedAs (IgnoreSQLite) in {
    persistence.create(migration).await
    val executor = mutactionExecutor(
      execute = {
        case _ => None
      },
      rollback = {
        case _ => None
      }
    )
    val applier = migrationApplier(mapper, executor)
    val result  = applier.apply(project, previousSchema = emptySchema, migration = migration).await
    result.succeeded should be(true)

    val persisted = persistence.getLastMigration(projectId).await.get
    persisted.status should be(MigrationStatus.Success)
    persisted.applied should be(migration.steps.size)
    persisted.rolledBack should be(0)
    persisted.startedAt.isDefined shouldEqual true
    persisted.finishedAt.isDefined shouldEqual true
  }

  "the applier" should "mark a migration as ROLLBACK_SUCCESS if all steps can be rolled back successfully" taggedAs (IgnoreSQLite) in {
    persistence.create(migration).await

    val executor = mutactionExecutor(
      execute = {
        case CreateModelTable(_, `step1Model`) => None
        case CreateModelTable(_, `step2Model`) => Some(new Exception("booom!"))
        case CreateModelTable(_, `step3Model`) => None
      },
      rollback = {
        case CreateModelTable(_, `step1Model`) => None
        case CreateModelTable(_, `step2Model`) => None
        case CreateModelTable(_, `step3Model`) => None
      }
    )
    val applier = migrationApplier(mapper, executor)
    val result  = applier.apply(project, previousSchema = emptySchema, migration = migration).await
    result.succeeded should be(false)

    val persisted = loadMigrationFromDb
    persisted.status should be(MigrationStatus.RollbackSuccess)
    persisted.applied should be(1)    // 1 step succeeded
    persisted.rolledBack should be(1) // 1 step was rolled back
  }

  "the applier" should "mark a migration as ROLLBACK_FAILURE if the rollback fails" taggedAs (IgnoreSQLite) in {
    persistence.create(migration).await

    val executor = mutactionExecutor(
      execute = {
        case CreateModelTable(_, `step1Model`) => None
        case CreateModelTable(_, `step2Model`) => None
        case CreateModelTable(_, `step3Model`) => Some(new Exception("booom!"))
      },
      rollback = {
        case CreateModelTable(_, `step1Model`) => Some(new Exception("booom!"))
        case CreateModelTable(_, `step2Model`) => None
        case CreateModelTable(_, `step3Model`) => None
      }
    )

    val applier = migrationApplier(mapper, executor)
    val result  = applier.apply(project, previousSchema = emptySchema, migration = migration).await
    result.succeeded should be(false)

    val persisted = loadMigrationFromDb
    persisted.status should be(MigrationStatus.RollbackFailure)
    persisted.applied should be(2)    // 2 steps succeeded
    persisted.rolledBack should be(1) // 1 steps were rolled back
  }

  def loadMigrationFromDb: Migration = persistence.byId(migration.id).await.get

  def migrationApplier(stepMapper: MigrationStepMapper, mutactionExecutor: DeployMutactionExecutor) = {
    MigrationApplierImpl(persistence, testDependencies.projectPersistence, stepMapper, mutactionExecutor, deployConnector.databaseInspector)
  }

//  lazy val succeedingSqlMutactionWithSucceedingRollback = clientSqlMutaction(succeedingStatementResult, rollback = succeedingStatementResult)
//  lazy val succeedingSqlMutactionWithFailingRollback    = clientSqlMutaction(succeedingStatementResult, rollback = failingStatementResult)
//  lazy val failingSqlMutactionWithSucceedingRollback    = clientSqlMutaction(failingStatementResult, rollback = succeedingStatementResult)
//  lazy val failingSqlMutactionWithFailingRollback       = clientSqlMutaction(failingStatementResult, rollback = failingStatementResult)
//  lazy val succeedingStatementResult                    = ClientSqlStatementResult[Any](DBIOAction.successful(()))
//  lazy val failingStatementResult                       = ClientSqlStatementResult[Any](DBIOAction.failed(new Exception("failing statement result")))

//  def clientSqlMutaction(execute: ClientSqlStatementResult[Any], rollback: ClientSqlStatementResult[Any]): ClientSqlMutaction = {
//    clientSqlMutaction(execute, Some(rollback))
//  }
//
//  def clientSqlMutaction(execute: ClientSqlStatementResult[Any], rollback: Option[ClientSqlStatementResult[Any]] = None): ClientSqlMutaction = {
//    val (executeArg, rollbackArg) = (execute, rollback)
//    new ClientSqlMutaction {
//      override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful(executeArg)
//
//      override def rollback: Option[Future[ClientSqlStatementResult[Any]]] = rollbackArg.map(Future.successful)
//    }
//  }

  def stepMapper(pf: PartialFunction[MigrationStep, Vector[DeployMutaction]]) = new MigrationStepMapper {
    override def mutactionFor(previousSchema: Schema, nextSchema: Schema, step: MigrationStep): Vector[DeployMutaction] = pf.apply(step)
  }

  def mutactionExecutor(
      execute: PartialFunction[DeployMutaction, Option[Throwable]],
      rollback: PartialFunction[DeployMutaction, Option[Throwable]]
  ): DeployMutactionExecutor = {
    val executePf  = execute
    val rollbackPf = rollback

    new DeployMutactionExecutor {
      override def execute(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema) = doit(executePf, mutaction)

      override def rollback(mutaction: DeployMutaction, schemaBeforeMigration: DatabaseSchema) = doit(rollbackPf, mutaction)

      def doit(pf: PartialFunction[DeployMutaction, Option[Throwable]], mutaction: DeployMutaction) = {
        pf.lift.apply(mutaction).flatten match {
          case Some(exception) => Future.failed[Unit](exception)
          case None            => Future.unit
        }
      }
    }
  }
}
