package com.prisma.api.connector.postgresql

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.{Databases, PostgresApiDatabaseMutationBuilder, PostgresDataResolver}
import com.prisma.api.connector.postgresql.impl._
import com.prisma.config.DatabaseConfig
import com.prisma.gc_values.{IdGCValue, RootGCValue}
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation

import scala.concurrent.{ExecutionContext, Future}

case class PassivePostgresApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases          = Databases.initialize(config)
  val activeConnector         = PostgresApiConnector(config)
  val activeMutactionExecutor = activeConnector.databaseMutactionExecutor

  override def initialize() = {
    databases
    Future.unit
  }

  override def shutdown() = {
    for {
      _ <- activeConnector.shutdown()
      _ <- databases.master.shutdown
      _ <- databases.readOnly.shutdown
    } yield ()
  }

  override def databaseMutactionExecutor: DatabaseMutactionExecutor = PassiveDatabaseMutactionExecutorImpl(activeMutactionExecutor, config.schema)
  override def dataResolver(project: Project)                       = PostgresDataResolver(project, databases.readOnly, schemaName = config.schema)
  override def masterDataResolver(project: Project)                 = PostgresDataResolver(project, databases.readOnly, schemaName = config.schema)

  override def projectIdEncoder: ProjectIdEncoder = activeConnector.projectIdEncoder

  override def capabilities = Vector.empty
}

case class PassiveDatabaseMutactionExecutorImpl(activeExecutor: PostgresDatabaseMutactionExecutor, schemaName: Option[String])(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {

  override def execute(mutactions: Vector[DatabaseMutaction], runTransactionally: Boolean): Future[Vector[DatabaseMutactionResult]] = {
    val transformed         = transform(mutactions)
    val interpreters        = transformed.map(interpreterFor)
    val combinedErrorMapper = interpreters.map(_.errorMapper).reduceLeft(_ orElse _)
    val mutationBuilder = PostgresApiDatabaseMutationBuilder(
      schemaName = schemaName.getOrElse(mutactions.head.project.id),
      schema = mutactions.head.project.schema
    )

    val singleAction = runTransactionally match {
      case true  => DBIO.sequence(interpreters.map(_.newAction(mutationBuilder))).transactionally
      case false => DBIO.sequence(interpreters.map(_.newAction(mutationBuilder)))
    }

    activeExecutor.clientDb
      .run(singleAction.withTransactionIsolation(TransactionIsolation.ReadCommitted))
      .recover { case error => throw combinedErrorMapper.lift(error).getOrElse(error) }
  }

  def transform(mutactions: Vector[DatabaseMutaction]): Vector[PassiveDatabaseMutaction] = {
    val replacements: Map[DatabaseMutaction, PassiveDatabaseMutaction] = mutactions
      .collect {
        case candidate: CreateDataItem =>
          val partner: Option[NestedCreateRelation] = mutactions
            .collect {
              case m: NestedCreateRelation => m
            }
            .find { m: NestedCreateRelation =>
              m.path.lastRelation_!.inlineManifestation match {
                case Some(manifestation: InlineRelationManifestation) =>
                  val mutactionsHaveTheSamePath = m.path == candidate.path
                  val wouldInsertIntoRightTable = manifestation.inTableOfModelId == m.path.lastModel.name
                  val isSelfRelation            = m.path.lastRelation_!.isSameModelRelation
                  mutactionsHaveTheSamePath && wouldInsertIntoRightTable && !isSelfRelation

                case None =>
                  false
              }
            }
          partner.map { p =>
            candidate -> NestedCreateDataItem(candidate, p)
          }
      }
      .flatten
      .toMap
    val removals: Vector[DatabaseMutaction] = replacements.values.toVector.flatMap(_.replaces)
    mutactions.collect {
      //case m if removals.contains(m) => N
      case m if replacements.contains(m) => replacements(m)
      case m if !removals.contains(m)    => PlainActiveDatabaseMutaction(m)
    }
  }

  def interpreterFor(mutaction: PassiveDatabaseMutaction): DatabaseMutactionInterpreter = mutaction match {
    case PlainActiveDatabaseMutaction(m: CreateDataItem) => CreateDataItemInterpreter(m, includeRelayRow = false)
    case PlainActiveDatabaseMutaction(m)                 => activeExecutor.interpreterFor(m)
    case m: NestedCreateDataItem                         => NestedCreateDataItemInterpreterForInlineRelations(m)
  }
}

sealed trait PassiveDatabaseMutaction {
  def replaces: Vector[DatabaseMutaction]
}
case class PlainActiveDatabaseMutaction(databaseMutaction: DatabaseMutaction) extends PassiveDatabaseMutaction {
  override def replaces = Vector.empty
}
case class NestedCreateDataItem(create: CreateDataItem, nestedCreateRelation: NestedCreateRelation) extends PassiveDatabaseMutaction {
  override def replaces = Vector(create, nestedCreateRelation)
}

case class NestedCreateDataItemInterpreterForInlineRelations(mutaction: NestedCreateDataItem) extends DatabaseMutactionInterpreter {
  import scala.concurrent.ExecutionContext.Implicits.global

  val project  = mutaction.create.project
  val path     = mutaction.create.path
  val relation = mutaction.nestedCreateRelation.path.lastRelation_!
  val model    = mutaction.create.path.lastModel

  require(relation.isInlineRelation)
  val inlineManifestation = relation.inlineManifestation.get
  require(inlineManifestation.inTableOfModelId == path.lastModel.name)

  override def action(mutationBuilder: PostgresApiDatabaseMutationBuilder): DBIO[Unit] = {
    val relationAction = DBIO.sequence(NestedCreateRelationInterpreter(mutaction.nestedCreateRelation).removalActions(mutationBuilder))
    val listAction     = mutationBuilder.setScalarList(path, mutaction.create.listArgs)
    DBIO.seq(relationAction, queryParentAndCreateDataItem(mutationBuilder), listAction)
  }

  import com.prisma.api.connector.postgresql.database.SlickExtensions._

  def queryParentAndCreateDataItem(mutationBuilder: PostgresApiDatabaseMutationBuilder) = {
    val idSubQuery      = mutationBuilder.pathQueryForLastParent(path)
    val lastParentModel = path.removeLastEdge.lastModel
    for {
      ids <- (sql"""select "#${lastParentModel.dbNameOfIdField_!}" from "#${mutationBuilder.schemaName}"."#${lastParentModel.dbName}" where "#${lastParentModel.dbNameOfIdField_!}" in (""" ++ idSubQuery ++ sql")")
              .as[String]
      result <- createDataItemWithLinkToParent(mutationBuilder)(ids.head)
    } yield result
  }

  def createDataItemWithLinkToParent(mutationBuilder: PostgresApiDatabaseMutationBuilder)(parentId: String) = {
    val inlineField  = relation.getFieldOnModel(model.name).get
    val argsMap      = mutaction.create.nonListArgs.raw.asRoot.map
    val modifiedArgs = argsMap.updated(inlineField.name, IdGCValue(parentId))
    mutationBuilder.createDataItem(path, PrismaArgs(RootGCValue(modifiedArgs)))
  }
}
