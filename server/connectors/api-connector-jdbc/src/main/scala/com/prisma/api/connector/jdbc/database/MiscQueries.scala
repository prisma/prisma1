package com.prisma.api.connector.jdbc.database

import java.sql.SQLException

import com.prisma.api.connector.Filter
import com.prisma.api.schema.APIErrors.ExecuteRawError
import org.jooq.impl.DSL.{name, table}
import play.api.libs.json._

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait MiscQueries extends BuilderBase with FilterConditionBuilder {
  import slickDatabase.profile.api._

  def countAllFromTable(tableName: String, whereFilter: Option[Filter]): DBIO[Int] = {

    lazy val query = {
      val aliasedTable = table(name(project.dbName, tableName)).as(topLevelAlias)
      val condition    = buildConditionForFilter(whereFilter)

      sql
        .selectCount()
        .from(aliasedTable)
        .where(condition)
    }

    queryToDBIO(query)(
      setParams = pp => SetParams.setFilter(pp, whereFilter),
      readResult = rs => {
        rs.next()
        rs.getInt(1)
      }
    )
  }

  def executeRaw(query: String)(implicit ec: ExecutionContext): DBIO[JsValue] =
    SimpleDBIO { ctx =>
      val ps          = ctx.connection.prepareStatement(query)
      val isResultSet = ps.execute()
      if (isResultSet) {
        val resultSet = ps.getResultSet
        val metaData  = resultSet.getMetaData
        val result    = mutable.Buffer.empty[JsValue]

        while (resultSet.next) {
          val keyValues = (1 to metaData.getColumnCount).map { i =>
            val columnName   = metaData.getColumnName(i)
            val untypedValue = resultSet.getObject(i)
            val value        = untypedValueToJson(untypedValue)
            columnName -> value
          }
          result += JsObject(keyValues)
        }
        JsArray(result)
      } else {
        JsNumber(ps.getUpdateCount)
      }
    }.asTry.map {
      case Success(x)               => x
      case Failure(e: SQLException) => throw ExecuteRawError(e)
      case Failure(e)               => throw e
    }

  private def untypedValueToJson(untypedValue: AnyRef): JsValue = {
    untypedValue match {
      case null                    => JsNull
      case v: String               => JsString(v)
      case v: java.lang.Boolean    => JsBoolean(v)
      case v: java.lang.Integer    => JsNumber(v.toInt)
      case v: java.lang.Long       => JsNumber(v.toLong)
      case v: java.lang.Float      => JsNumber(v.toDouble)
      case v: java.lang.Double     => JsNumber(v.toDouble)
      case v: java.math.BigDecimal => JsNumber(v)
      case v: java.sql.Timestamp   => JsString(v.toString)
      case v: java.sql.Time        => JsString(v.toString)
      case v: java.sql.Date        => JsString(v.toString)
      case v: java.sql.Blob        => ???
      case v: java.sql.NClob       => ???
      case v: java.sql.Clob        => ???
      case v: java.sql.RowId       => ???
      case v: java.sql.SQLXML      => JsString(v.getString)
      case v: java.sql.Array =>
        val array = v.getArray.asInstanceOf[Array[AnyRef]]
        JsArray(array.map(untypedValueToJson))
    }
  }

}
