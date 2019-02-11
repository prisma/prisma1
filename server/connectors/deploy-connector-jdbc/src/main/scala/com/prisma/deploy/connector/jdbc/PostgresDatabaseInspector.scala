package com.prisma.deploy.connector.jdbc
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.Index
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext

case class PostgresDatabaseInspector(db: SlickDatabase)(implicit val ec: ExecutionContext) extends DatabaseInspectorBase {
  import db.profile.api.actionBasedSQLInterpolation

  override def getSequences(schema: String, table: String): DBIO[Vector[IntrospectedSequence]] = {
    val sequencesForTable = sql"""
           |SELECT
           |    cols.column_name,
           |    seq.sequence_name,
           |    seq.start_value
           |FROM
           |    information_schema.columns AS cols,
           |    information_schema.sequences AS seq
           |WHERE
           |    column_default LIKE '%' || seq.sequence_name || '%'
           |    AND sequence_schema = '#$schema'
           |    AND cols.table_name = '#$table';
         """.stripMargin.as[(String, String, Int)]

    def currentValue(sequence: String) = sql"""select last_value FROM "#$schema"."#$sequence";""".as[Int].head

    val action = for {
      sequences     <- sequencesForTable
      currentValues <- DBIO.sequence(sequences.map(t => currentValue(t._2)))
    } yield {
      sequences.zip(currentValues).map {
        case ((column, sequence, _), current) =>
          IntrospectedSequence(column, sequence, current)
      }
    }
    action.withPinnedSession
  }

  override def foreignKeyConstraints(schema: String, table: String): DBIO[Vector[IntrospectedForeignKey]] = {
    sql"""
         |SELECT
         |	  kcu.constraint_name as "fkConstraintName",
         |    kcu.table_name as "fkTableName",
         |    kcu.column_name as "fkColumnName",
         |    ccu.table_name as "referencedTableName",
         |    ccu.column_name as "referencedColumnName"
         |FROM
         |    information_schema.key_column_usage kcu
         |INNER JOIN
         |	information_schema.constraint_column_usage AS ccu
         |	ON ccu.constraint_catalog = kcu.constraint_catalog
         |    AND ccu.constraint_schema = kcu.constraint_schema
         |    AND ccu.constraint_name = kcu.constraint_name
         |INNER JOIN
         |	information_schema.referential_constraints as rc
         |	ON rc.constraint_catalog = kcu.constraint_catalog
         |    AND rc.constraint_schema = kcu.constraint_schema
         |    AND rc.constraint_name = kcu.constraint_name
         |WHERE
         |	kcu.table_schema = $schema AND
         |	kcu.table_name = $table
            """.stripMargin.as[IntrospectedForeignKey]
  }

  override def indexes(schema: String, table: String): DBIO[Vector[Index]] = {
    sql"""
         |SELECT
         |    tableInfos.relname as table_name,
         |    indexInfos.relname as index_name,
         |    array_agg(columnInfos.attname) as column_names,
         |    rawIndex.indisunique as is_unique,
         |    rawIndex.indisprimary as is_primary_key
         |FROM
         |    -- pg_class stores infos about tables, indices etc: https://www.postgresql.org/docs/9.3/catalog-pg-class.html
         |    pg_class tableInfos,
         |    pg_class indexInfos,
         |    -- pg_index stores indices: https://www.postgresql.org/docs/9.3/catalog-pg-index.html
         |    pg_index rawIndex,
         |    -- pg_attribute stores infos about columns: https://www.postgresql.org/docs/9.3/catalog-pg-attribute.html
         |    pg_attribute columnInfos,
         |    -- pg_namespace stores info about the schema
         |    pg_namespace schemaInfo
         |WHERE
         |    -- find table info for index
         |    tableInfos.oid = rawIndex.indrelid
         |    -- find index info
         |    AND indexInfos.oid = rawIndex.indexrelid
         |    -- find table columns
         |    AND columnInfos.attrelid = tableInfos.oid
         |    AND columnInfos.attnum = ANY(rawIndex.indkey)
         |    -- we only consider oridnary tables
         |    AND tableInfos.relkind = 'r'
         |    -- we only consider stuff out of one specific schema
         |    AND tableInfos.relnamespace = schemaInfo.oid
         |GROUP BY
         |    tableInfos.relname,
         |    indexInfos.relname,
         |    rawIndex.indisunique,
         |    rawIndex.indisprimary
            """.stripMargin.as[Index]
  }
}
