package com.prisma.api.connector.jdbc.database

import java.sql.ResultSet

import com.prisma.api.connector.jdbc.extensions.{JdbcExtensions, JooqExtensions, SlickExtensions}
import com.prisma.api.connector.{PrismaNode, PrismaNodeWithParent}
import com.prisma.gc_values.RootGCValue
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models._
import com.prisma.slick.NewJdbcExtensions.ReadsResultSet
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL.{field, name, table}
import org.jooq.{Field, Query => JooqQuery, _}
import slick.jdbc.{MySQLProfile, PositionedParameters, PostgresProfile}

import scala.collection.JavaConverters._

trait BuilderBase extends JooqExtensions with JdbcExtensions with SlickExtensions with ResultSetReaders {
  import JooqQueryBuilders.placeHolder

  def schemaName: String

  val slickDatabase: SlickDatabase
  val dialect: SQLDialect = slickDatabase.profile match {
    case PostgresProfile => SQLDialect.POSTGRES_9_5
    case MySQLProfile    => SQLDialect.MYSQL_5_7
    case x               => sys.error(s"No Jooq SQLDialect for Slick profile $x configured yet")
  }

  import slickDatabase.profile.api._

  val sql = DSL.using(dialect, new Settings().withRenderFormatted(true))

  private val relayIdTableName = "_RelayId"

  val relayIdColumn                                                                         = field(name(schemaName, relayIdTableName, "id"))
  val relayStableIdentifierColumn                                                           = field(name(schemaName, relayIdTableName, "stableModelIdentifier"))
  val relayTable                                                                            = table(name(schemaName, relayIdTableName))
  def idField(model: Model)                                                                 = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def modelTable(model: Model)                                                              = table(name(schemaName, model.dbName))
  def relationTable(relation: Relation)                                                     = table(name(schemaName, relation.relationTableName))
  def scalarListTable(field: ScalarField)                                                   = table(name(schemaName, scalarListTableName(field)))
  def modelColumn(model: Model, scalarField: com.prisma.shared.models.Field): Field[AnyRef] = field(name(schemaName, model.dbName, scalarField.dbName))
  def modelIdColumn(model: Model)                                                           = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def relationColumn(relation: Relation, side: RelationSide.Value)                          = field(name(schemaName, relation.relationTableName, relation.columnForRelationSide(side)))
  def relationIdColumn(relation: Relation)                                                  = field(name(schemaName, relation.relationTableName, "id"))
  def inlineRelationColumn(relation: Relation, mani: InlineRelationManifestation)           = field(name(schemaName, relation.relationTableName, mani.referencingColumn))
  def scalarListColumn(scalarField: ScalarField, column: String)                            = field(name(schemaName, scalarListTableName(scalarField), column))
  def column(table: String, column: String)                                                 = field(name(schemaName, table, column))
  def aliasColumn(column: String)                                                           = field(name(JooqQueryBuilders.topLevelAlias, column))
  def placeHolders(vector: Iterable[Any])                                                   = vector.toList.map(_ => placeHolder).asJava
  private def scalarListTableName(field: ScalarField)                                       = field.model.dbName + "_" + field.dbName

  val isMySql = dialect.family() == SQLDialect.MYSQL

  def queryToDBIO[T](query: JooqQuery)(setParams: PositionedParameters => Unit, readResult: ResultSet => T): DBIO[T] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      val pp = new PositionedParameters(ps)
      setParams(pp)

      val rs = ps.executeQuery()
      readResult(rs)
    }
  }

  def deleteToDBIO(query: Delete[Record])(setParams: PositionedParameters => Unit): DBIO[Unit] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      val pp = new PositionedParameters(ps)
      setParams(pp)

      ps.execute()
    }
  }

  def updateToDBIO(query: Update[Record])(setParams: PositionedParameters => Unit): DBIO[Unit] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      val pp = new PositionedParameters(ps)
      setParams(pp)

      ps.executeUpdate()
    }
  }

  def truncateToDBIO(query: Truncate[Record]): DBIO[Unit] = {
    SimpleDBIO { ctx =>
      val ps = ctx.connection.prepareStatement(query.getSQL)
      ps.executeUpdate()
    }
  }
}
