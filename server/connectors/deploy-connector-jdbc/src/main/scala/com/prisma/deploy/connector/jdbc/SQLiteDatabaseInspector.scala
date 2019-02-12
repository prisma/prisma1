package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.{MySqlDialect, PostgresDialect, SlickDatabase}
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.jdbc.DatabaseInspectorBase.{IntrospectedColumn, IntrospectedForeignKey, IntrospectedSequence}
import com.prisma.shared.models.TypeIdentifier
import slick.dbio.DBIO
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteDatabaseInspector(db: SlickDatabase)(implicit val ec: ExecutionContext) extends DatabaseInspector {
  import db.profile.api.actionBasedSQLInterpolation

  override def inspect(schema: String): Future[DatabaseSchema] = db.database.run(action(schema))

  def action(schema: String): DBIO[DatabaseSchema] = {
    for {
      tableNames <- getTableNames(schema)
      tables     <- DBIO.sequence(tableNames.map(name => getTable(schema, name)))
    } yield {
      DatabaseSchema(tables)
    }
  }

  private def getTableNames(schema: String): DBIO[Vector[String]] = {
    sql"""
         |SELECT
         |  table_name
         |FROM
         |  information_schema.tables
         |WHERE
         |  table_schema = $schema AND
         |  -- Views are not supported yet
         |  table_type = 'BASE TABLE'
       """.stripMargin.as[String]
  }

  private def getTable(schema: String, table: String): DBIO[Table] = {
    for {
      introspectedColumns     <- getColumns(schema, table)
      introspectedForeignKeys <- foreignKeyConstraints(schema, table)
      introspectedIndexes     <- indexes(schema, table)
      sequences               <- getSequences(schema, table)
    } yield {
      val columns = introspectedColumns.map { col =>
        // this needs to be extended further in the future if we support arbitrary SQL types
        val typeIdentifier = typeIdentifierForTypeName(col.udtName).getOrElse {
          sys.error(s"Encountered unknown SQL type ${col.udtName} with column ${col.name}. $col")
        }
        val fk = introspectedForeignKeys.find(fk => fk.column == col.name).map { fk =>
          ForeignKey(fk.referencedTable, fk.referencedColumn)
        }
        val sequence = sequences.find(_.column == col.name).map { mseq =>
          Sequence(mseq.name, mseq.current)
        }
        Column(
          name = col.name,
          tpe = col.udtName,
          typeIdentifier = typeIdentifier,
          isRequired = !col.isNullable,
          foreignKey = fk,
          sequence = sequence
        )(_)
      }
      Table(table, columns, indexes = introspectedIndexes)
    }
  }

  private def getColumns(schema: String, table: String): DBIO[Vector[IntrospectedColumn]] = {
    sql"""
         |SELECT
         |  cols.ordinal_position,
         |  cols.column_name,
         |  cols.#$dataTypeColumn,
         |  cols.column_default,
         |  cols.is_nullable = 'YES' as is_nullable
         |FROM
         |  information_schema.columns AS cols
         |WHERE
         |  cols.table_schema = $schema
         |  AND cols.table_name  = $table
          """.stripMargin.as[IntrospectedColumn]
  }

  /**
    * RESULT CONVERTERS
    */
  implicit lazy val introspectedColumnGetResult: GetResult[IntrospectedColumn] = GetResult { ps =>
    IntrospectedColumn(
      name = ps.rs.getString("column_name"),
      udtName = ps.rs.getString(dataTypeColumn),
      default = ps.rs.getString("column_default"),
      isNullable = ps.rs.getBoolean("is_nullable")
    )
  }

  /**
    * Other Helpers
    */
  private val dataTypeColumn = db.prismaDialect match {
    case PostgresDialect => "udt_name"
    case MySqlDialect    => "DATA_TYPE"
    case x               => sys.error(s"$x is not implemented yet.")
  }

  private def typeIdentifierForTypeName(typeName: String): Option[TypeIdentifier.ScalarTypeIdentifier] = {
    // https://dev.mysql.com/doc/refman/8.0/en/data-types.html
    typeName match {
      case "tinyint"                                  => Some(TypeIdentifier.Boolean)
      case _ if typeName.contains("char")             => Some(TypeIdentifier.String)
      case _ if typeName.contains("text")             => Some(TypeIdentifier.String)
      case _ if typeName.contains("int")              => Some(TypeIdentifier.Int)
      case "decimal" | "numeric" | "float" | "double" => Some(TypeIdentifier.Float)
      case "datetime" | "timestamp"                   => Some(TypeIdentifier.DateTime)
      case _                                          => None
    }
  }

  private def getSequences(schema: String, table: String): DBIO[Vector[IntrospectedSequence]] = {
    val sequencesForTable =
      sql"""
           |SELECT
           |    column_name
           |FROM
           |    information_schema.COLUMNS
           |WHERE
           |    extra = 'auto_increment'
           |    AND table_name = $table
           |    AND table_schema = $schema;
         """.stripMargin.as[String]
    val currentValueForSequence =
      sql"""
           |SELECT
           |    AUTO_INCREMENT
           |FROM
           |    information_schema.TABLES
           |WHERE
           |    table_name = $table
           |    AND table_schema = $schema;
         """.stripMargin.as[Int]

    for {
      sequences     <- sequencesForTable
      currentValues <- currentValueForSequence
    } yield {
      val x = for {
        column       <- sequences.headOption
        currentValue <- currentValues.headOption
      } yield IntrospectedSequence(column = column, name = "sequences_are_not_named_in_mysql", current = currentValue)
      x.toVector
    }
  }

  private def foreignKeyConstraints(schema: String, table: String): DBIO[Vector[IntrospectedForeignKey]] = {
    sql"""
         |SELECT
         |    kcu.constraint_name AS fkConstraintName,
         |    kcu.table_name AS fkTablename,
         |    kcu.column_name AS fkColumnName,
         |    kcu.referenced_table_name AS referencedTableName,
         |    kcu.referenced_column_name AS referencedColumnName
         |FROM
         |    information_schema.key_column_usage kcu
         |WHERE
         |    kcu.constraint_schema = $schema
         |    AND kcu.referenced_table_name IS NOT NULL;
            """.stripMargin.as[IntrospectedForeignKey]
  }

  private def indexes(schema: String, table: String): DBIO[Vector[Index]] = {
    sql"""
         |SELECT
         |  table_name,
         |  index_name,
         |  GROUP_CONCAT(DISTINCT column_name SEPARATOR ', ') AS column_names,
         |  NOT non_unique AS is_unique,
         |  index_name = 'PRIMARY' AS is_primary_key
         |FROM
         |  information_schema.statistics
         |WHERE
         |  table_schema = $schema
         |  AND table_name = $table
         |GROUP BY
         |  table_name, index_name, non_unique
         """.stripMargin.as[(String, String, String, Boolean, Boolean)].map { rows =>
      rows.map { row =>
        Index(
          name = row._2,
          columns = row._3.split(',').map(_.trim).toVector,
          unique = row._4
        )
      }
    }
  }
}
