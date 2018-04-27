package com.prisma.api.connector.postgresql

import java.sql.PreparedStatement

import com.prisma.api.connector._
import com.prisma.api.connector.postgresql.database.{Databases, PostGresApiDatabaseMutationBuilder}
import com.prisma.api.connector.postgresql.impl._
import com.prisma.config.DatabaseConfig
import com.prisma.gc_values.NullGCValue
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation

import scala.concurrent.{ExecutionContext, Future}

case class PassivePostgresApiConnector(config: DatabaseConfig)(implicit ec: ExecutionContext) extends ApiConnector {
  lazy val databases = Databases.initialize(config)

  val activeConnector = PostgresApiConnector(config)

  override def initialize() = {
    databases
    Future.unit
  }

  override def shutdown() = {
    for {
      _ <- databases.master.shutdown
      _ <- databases.readOnly.shutdown
    } yield ()
  }

  override def databaseMutactionExecutor: DatabaseMutactionExecutor =
    PassiveDatabaseMutactionExecutorImpl(activeConnector.databaseMutactionExecutor.asInstanceOf[DatabaseMutactionExecutorImpl])
  override def dataResolver(project: Project)       = activeConnector.dataResolver(project)
  override def masterDataResolver(project: Project) = activeConnector.masterDataResolver(project)

  override def projectIdEncoder: ProjectIdEncoder = activeConnector.projectIdEncoder
}

case class PassiveDatabaseMutactionExecutorImpl(activeExecutor: DatabaseMutactionExecutorImpl)(implicit ec: ExecutionContext)
    extends DatabaseMutactionExecutor {

  override def execute(mutactions: Vector[DatabaseMutaction], runTransactionally: Boolean): Future[Unit] = {
    val transformed         = transform(mutactions)
    val interpreters        = transformed.map(interpreterFor)
    val combinedErrorMapper = interpreters.map(_.errorMapper).reduceLeft(_ orElse _)

    val singleAction = runTransactionally match {
      case true  => DBIO.seq(interpreters.map(_.action): _*).transactionally
      case false => DBIO.seq(interpreters.map(_.action): _*)
    }

    activeExecutor.clientDb
      .run(singleAction.withTransactionIsolation(TransactionIsolation.ReadCommitted))
      .recover { case error => throw combinedErrorMapper.lift(error).getOrElse(error) }
      .map(_ => ())
  }

  def transform(mutactions: Vector[DatabaseMutaction]): Vector[PassiveDatabaseMutaction] = {
    val replacements: Map[DatabaseMutaction, PassiveDatabaseMutaction] = mutactions
      .collect {
        case candidate: CreateDataItem =>
          val partner: Option[NestedCreateRelation] = mutactions.collectFirst {
            case m: NestedCreateRelation if m.path == candidate.path => m
          }
          partner.map { p =>
            candidate -> NestedCreateDataItem(candidate, p)
          }
      }
      .collect {
        case Some(x) => x
      }
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
    case m: NestedCreateDataItem                         => NestedCreateDataItemInterpreter(m)
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

case class NestedCreateDataItemInterpreter(mutaction: NestedCreateDataItem) extends DatabaseMutactionInterpreter {
  import scala.concurrent.ExecutionContext.Implicits.global

  val project = mutaction.create.project
  val path    = mutaction.create.path

  override val action = {
//    val createNonList = PostGresApiDatabaseMutationBuilder.createDataItem(project.id, path, mutaction.create.nonListArgs)
//    val listAction    = PostGresApiDatabaseMutationBuilder.setScalarList(project.id, path, mutaction.listArgs)
    DBIO.seq(bla)
  }

  import com.prisma.api.connector.postgresql.database.JdbcExtensions._
  import com.prisma.api.connector.postgresql.database.SlickExtensions._

  def bla = {
    val idSubQuery = PostGresApiDatabaseMutationBuilder.pathQueryForLastParent(project.id, path)

    for {
      ids <- (sql"""select "id" from #${project.id}.#${path.removeLastEdge.lastModel.pgTableName} where id in (""" ++ idSubQuery ++ sql")").as[String]
      _   <- createDataItemAndLinkToParent(ids.head)
    } yield ()
  }

  def createDataItemAndLinkToParent(parentId: String) = {
    val projectId = project.id
    val args      = mutaction.create.nonListArgs
    val relation  = mutaction.nestedCreateRelation.path.lastRelation_!
    val relationField = if (path.lastModel.id == relation.modelAId) {
      relation.getModelAField(project.schema)
    } else {
      relation.getModelBField(project.schema)
    }

    SimpleDBIO[Unit] { x =>
//      val subselect    = PostGresApiDatabaseMutationBuilder.pathQueryForLastParent(projectId, path)
      val columns      = path.lastModel.scalarNonListFields.map(_.name) :+ relationField.get.name
      val escapedKeys  = columns.map(column => s""""$column"""").mkString(",")
      val placeHolders = columns.map(_ => "?").mkString(",")

      val query                         = s"""INSERT INTO "$projectId"."${path.lastModel.pgTableName}" ($escapedKeys) VALUES ($placeHolders, $parentId)"""
      val itemInsert: PreparedStatement = x.connection.prepareStatement(query)

      columns.zipWithIndex.foreach {
        case (column, index) =>
          args.raw.asRoot.map.get(column) match {
            case Some(NullGCValue) if column == "createdAt" || column == "updatedAt" => itemInsert.setTimestamp(index + 1, currentTimeStampUTC)
            case Some(gCValue)                                                       => itemInsert.setGcValue(index + 1, gCValue)
            case None if column == "createdAt" || column == "updatedAt"              => itemInsert.setTimestamp(index + 1, currentTimeStampUTC)
            case None                                                                => itemInsert.setNull(index + 1, java.sql.Types.NULL)
          }
      }
      itemInsert.execute()
    }
  }

//  override val errorMapper = {
//    case e: PSQLException if e.getSQLState == "23505" && GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).isDefined =>
//      APIErrors.UniqueConstraintViolation(path.lastModel.name, GetFieldFromSQLUniqueException.getFieldOption(mutaction.nonListArgs.keys, e).get)
//    case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1452 =>
//      APIErrors.NodeDoesNotExist("")
//  }
}
