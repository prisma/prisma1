package com.prisma.api.connector.jdbc.database

import java.sql.{PreparedStatement, ResultSet, Statement}

import com.prisma.api.connector.jdbc.extensions.{JdbcExtensions, JooqExtensions, SlickExtensions}
import com.prisma.shared.models.Manifestations.InlineRelationManifestation
import com.prisma.shared.models._
import com.prisma.slick.ResultSetExtensions
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DSL.{field, name, table}
import org.jooq.{Field, Query => JooqQuery, _}
import slick.jdbc.{MySQLProfile, PositionedParameters, PostgresProfile}

import scala.collection.JavaConverters._

trait BuilderBase extends JooqExtensions with JdbcExtensions with SlickExtensions with ResultSetReaders with QueryBuilderConstants with ResultSetExtensions {

  def schemaName: String
  val slickDatabase: SlickDatabase

  val dialect: SQLDialect = slickDatabase.profile match {
    case PostgresProfile => SQLDialect.POSTGRES_9_5
    case MySQLProfile    => SQLDialect.MYSQL_5_7
    case x               => sys.error(s"No Jooq SQLDialect for Slick profile $x configured yet")
  }

  val isMySql    = dialect.family() == SQLDialect.MYSQL
  val isPostgres = dialect.family() == SQLDialect.POSTGRES

  import slickDatabase.profile.api._

  val sql = DSL.using(dialect, new Settings().withRenderFormatted(true))

  private val relayIdTableName                                                    = "_RelayId"
  val relayIdColumn                                                               = field(name(schemaName, relayIdTableName, "id"))
  val relayStableIdentifierColumn                                                 = field(name(schemaName, relayIdTableName, "stableModelIdentifier"))
  val relayTable                                                                  = table(name(schemaName, relayIdTableName))
  def idField(model: Model)                                                       = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def modelTable(model: Model)                                                    = table(name(schemaName, model.dbName))
  def relationTable(relation: Relation)                                           = table(name(schemaName, relation.relationTableName))
  def scalarListTable(field: ScalarField)                                         = table(name(schemaName, scalarListTableName(field)))
  def modelColumn(fieldModel: com.prisma.shared.models.Field): Field[AnyRef]      = field(name(schemaName, fieldModel.model.dbName, fieldModel.dbName))
  def modelIdColumn(model: Model)                                                 = field(name(schemaName, model.dbName, model.dbNameOfIdField_!))
  def modelIdColumn(alias: String, model: Model)                                  = field(name(alias, model.idField_!.dbName))
  def relationColumn(relation: Relation, side: RelationSide.Value)                = field(name(schemaName, relation.relationTableName, relation.columnForRelationSide(side)))
  def relationIdColumn(relation: Relation)                                        = field(name(schemaName, relation.relationTableName, "id"))
  def inlineRelationColumn(relation: Relation, mani: InlineRelationManifestation) = field(name(schemaName, relation.relationTableName, mani.referencingColumn))
  def scalarListColumn(scalarField: ScalarField, column: String)                  = field(name(schemaName, scalarListTableName(scalarField), column))
  def column(table: String, column: String)                                       = field(name(schemaName, table, column))
  def aliasColumn(column: String)                                                 = field(name(topLevelAlias, column))
  def aliasColumn(scalarField: ScalarField)                                       = field(name(topLevelAlias, scalarField.dbName))
  def placeHolders(vector: Iterable[Any])                                         = vector.toList.map(_ => placeHolder).asJava
  private def scalarListTableName(field: ScalarField)                             = field.model.dbName + "_" + field.dbName

  def queryToDBIO[T](query: JooqQuery)(setParams: PositionedParameters => Unit, readResult: ResultSet => T): DBIO[T] = {
    jooqToDBIO(query, setParams) { ps =>
      val rs = ps.executeQuery()
      readResult(rs)
    }
  }
  def insertReturningGeneratedKeysToDBIO[T](query: Insert[Record])(setParams: PositionedParameters => Unit, readResult: ResultSet => T): DBIO[T] = {
    jooqToDBIO(query, setParams, returnGeneratedKeys = true)(
      statementFn = { ps =>
        ps.execute()
        readResult(ps.getGeneratedKeys)
      }
    )
  }
  def insertToDBIO[T](query: Insert[Record])(setParams: PositionedParameters => Unit): DBIO[Unit] = jooqToDBIO(query, setParams)(_.execute())
  def deleteToDBIO(query: Delete[Record])(setParams: PositionedParameters => Unit): DBIO[Unit]    = jooqToDBIO(query, setParams)(_.execute())
  def updateToDBIO(query: Update[Record])(setParams: PositionedParameters => Unit): DBIO[Unit]    = jooqToDBIO(query, setParams)(_.executeUpdate)
  def truncateToDBIO(query: Truncate[Record]): DBIO[Unit]                                         = jooqToDBIO(query, _ => ())(_.executeUpdate())

  private def jooqToDBIO[T](
      query: JooqQuery,
      setParams: PositionedParameters => Unit,
      returnGeneratedKeys: Boolean = false
  )(statementFn: PreparedStatement => T): DBIO[T] = {
    SimpleDBIO { ctx =>
      val ps = if (returnGeneratedKeys) {
        ctx.connection.prepareStatement(query.getSQL, Statement.RETURN_GENERATED_KEYS)
      } else {
        ctx.connection.prepareStatement(query.getSQL)
      }
      val pp = new PositionedParameters(ps)
      setParams(pp)
      statementFn(ps)
    }
  }
}
