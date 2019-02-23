package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.{MySqlDialect, PostgresDialect, SlickDatabase, SqliteDialect}
import com.prisma.deploy.connector._
import com.prisma.deploy.connector.jdbc.DatabaseInspectorBase.{IntrospectedColumn, IntrospectedForeignKey}
import com.prisma.shared.models.TypeIdentifier
import slick.dbio.DBIO
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

case class SQLiteDatabaseInspector(db: SlickDatabase)(implicit val ec: ExecutionContext) extends DatabaseInspector {
  import db.profile.api.actionBasedSQLInterpolation

  //attach here

  override def inspect(schema: String): Future[DatabaseSchema] = {
    val list   = sql"""PRAGMA database_list;""".as[(String, String, String)]
    val path   = s"""'db/$schema'"""
    val attach = sqlu"ATTACH DATABASE #${path} AS #${schema};"

    val attachIfNecessary = for {
      attachedDbs <- list
      _ <- attachedDbs.map(_._2).contains(schema) match {
            case true  => slick.dbio.DBIO.successful(())
            case false => attach
          }
      result <- action(schema)
    } yield result
    db.database.run(attachIfNecessary.withPinnedSession)
  }

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
         |  name
         |FROM
         |  #${schema}.sqlite_master
         |WHERE
         |  type='table'
       """.stripMargin.as[String]
  }

  private def getTable(schema: String, table: String): DBIO[Table] = {
    for {
      introspectedColumns     <- getColumns(schema, table)
      introspectedForeignKeys <- foreignKeyConstraints(schema, table)
//      introspectedIndexes     <- indexes(schema, table)
//      sequences               <- getSequences(schema, table)
    } yield {
      val columns = introspectedColumns.map { col =>
        // this needs to be extended further in the future if we support arbitrary SQL types
        val typeIdentifier = typeIdentifierForTypeName(col.udtName).getOrElse {
          sys.error(s"Encountered unknown SQL type ${col.udtName} with column ${col.name}. $col")
        }
        val fk: Option[ForeignKey] = introspectedForeignKeys.find(fk => fk.column == col.name).map { fk =>
          ForeignKey(fk.referencedTable, fk.referencedColumn)
        }
        val sequence: Option[Sequence] = None

//sequences.find(_.column == col.name).map { mseq =>
//          Sequence(mseq.name, mseq.current)
//        }

        Column(
          name = col.name,
          tpe = col.udtName,
          typeIdentifier = typeIdentifier,
          isRequired = col.isNullable,
          foreignKey = fk,
          sequence = sequence
        )(_)
      }
      Table(table, columns, indexes = Vector.empty) // introspectedIndexes)
    }
  }

  private def getColumns(schema: String, table: String): DBIO[Vector[IntrospectedColumn]] = {
    sql"""Pragma "#$schema".table_info ("#$table")""".stripMargin.as[IntrospectedColumn]
  }

  /**
    * RESULT CONVERTERS
    */
  implicit lazy val introspectedColumnGetResult: GetResult[IntrospectedColumn] = GetResult { ps =>
    IntrospectedColumn(
      name = ps.rs.getString("name"),
      udtName = ps.rs.getString(dataTypeColumn),
      default = ps.rs.getString("dflt_value"),
      isNullable = ps.rs.getBoolean("notnull")
    )
  }

  /**
    * Other Helpers
    */
  private val dataTypeColumn = db.prismaDialect match {
    case PostgresDialect => "udt_name"
    case MySqlDialect    => "DATA_TYPE"
    case SqliteDialect   => "type"
    case x               => sys.error(s"$x is not implemented yet.")
  }

  private def typeIdentifierForTypeName(typeName: String): Option[TypeIdentifier.ScalarTypeIdentifier] = {
    typeName match {
      case "boolean"                                                     => Some(TypeIdentifier.Boolean)
      case "bool"                                                        => Some(TypeIdentifier.Boolean)
      case _ if typeName.contains("char")                                => Some(TypeIdentifier.String)
      case _ if typeName.contains("text")                                => Some(TypeIdentifier.String)
      case _ if typeName.contains("int")                                 => Some(TypeIdentifier.Int)
      case _ if typeName.contains("INTEGER")                             => Some(TypeIdentifier.Int)
      case _ if typeName.contains("datetime")                            => Some(TypeIdentifier.DateTime)
      case "decimal" | "numeric" | "float" | "double" | "Decimal(65,30)" => Some(TypeIdentifier.Float)
      case _                                                             => None
    }
  }

//  private def getSequences(schema: String, table: String): DBIO[Vector[IntrospectedSequence]] = {
//    val sequencesForTable =
//      sql"""
//           |SELECT
//           |    column_name
//           |FROM
//           |    information_schema.COLUMNS
//           |WHERE
//           |    extra = 'auto_increment'
//           |    AND table_name = $table
//           |    AND table_schema = $schema;
//         """.stripMargin.as[String]
//
//    val currentValueForSequence =
//      sql"""
//           |SELECT
//           |    AUTO_INCREMENT
//           |FROM
//           |    information_schema.TABLES
//           |WHERE
//           |    table_name = $table
//           |    AND table_schema = $schema;
//         """.stripMargin.as[Int]
//
//    for {
//      sequences     <- sequencesForTable
//      currentValues <- currentValueForSequence
//    } yield {
//      val x = for {
//        column       <- sequences.headOption
//        currentValue <- currentValues.headOption
//      } yield IntrospectedSequence(column = column, name = "sequences_are_not_named_in_mysql", current = currentValue)
//      x.toVector
//    }
//  }
//
  private def foreignKeyConstraints(schema: String, table: String): DBIO[Vector[IntrospectedForeignKey]] = {
    implicit val introspectedForeignKeyGetResult: GetResult[IntrospectedForeignKey] = GetResult { pr =>
      IntrospectedForeignKey(
        name = table ++ "_" ++ pr.rs.getString("from") ++ "_fk",
        table = table,
        column = pr.rs.getString("from"),
        referencedTable = pr.rs.getString("table"),
        referencedColumn = pr.rs.getString("to")
      )
    }

    sql"""Pragma "#$schema".foreign_key_list("#$table");""".stripMargin.as[IntrospectedForeignKey]
  }
//
//  private def indexes(schema: String, table: String): DBIO[Vector[Index]] = {
//    sql"""
//         |SELECT
//         |  table_name,
//         |  index_name,
//         |  GROUP_CONCAT(DISTINCT column_name SEPARATOR ', ') AS column_names,
//         |  NOT non_unique AS is_unique,
//         |  index_name = 'PRIMARY' AS is_primary_key
//         |FROM
//         |  information_schema.statistics
//         |WHERE
//         |  table_schema = $schema
//         |  AND table_name = $table
//         |GROUP BY
//         |  table_name, index_name, non_unique
//         """.stripMargin.as[(String, String, String, Boolean, Boolean)].map { rows =>
//      rows.map { row =>
//        Index(
//          name = row._2,
//          columns = row._3.split(',').map(_.trim).toVector,
//          unique = row._4
//        )
//      }
//    }
//  }
}
