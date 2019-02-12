package com.prisma.deploy.connector.jdbc
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.Index
import com.prisma.deploy.connector.jdbc.DatabaseInspectorBase._
import com.prisma.shared.models.TypeIdentifier
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class MySqlDatabaseInspector(db: SlickDatabase)(implicit val ec: ExecutionContext) extends DatabaseInspectorBase {
  import db.profile.api.actionBasedSQLInterpolation

  override protected def typeIdentifierForTypeName(typeName: String): Option[TypeIdentifier.ScalarTypeIdentifier] = {
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

  override def getSequences(schema: String, table: String): DBIO[Vector[IntrospectedSequence]] = {
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

  override def foreignKeyConstraints(schema: String, table: String): DBIO[Vector[IntrospectedForeignKey]] = {
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

  override def indexes(schema: String, table: String): DBIO[Vector[Index]] = {
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
