package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.{MySqlDialect, PostgresDialect, SlickDatabase, SqliteDialect}
import com.prisma.deploy.connector._
import com.prisma.shared.models.TypeIdentifier
import slick.dbio.DBIO
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

case class DatabaseInspectorImpl(db: SlickDatabase)(implicit ec: ExecutionContext) extends DatabaseInspector {
  import db.profile.api.actionBasedSQLInterpolation

  // intermediate helper classes
  case class IntrospectedColumn(name: String, udtName: String, default: String, isNullable: Boolean, isUnique: Boolean)
  case class IntrospectedForeignKey(name: String, table: String, column: String, referencedTable: String, referencedColumn: String)
  case class IntrospectedSequence(column: String, name: String, current: Int)

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
         |        table_name
         |      FROM
         |        information_schema.tables
         |      WHERE
         |        table_schema = $schema
         |        -- Views are not supported yet
         |        AND table_type = 'BASE TABLE'
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
        val typeIdentifier = col.udtName match {
          case "varchar" | "string" | "text" | "bpchar"             => TypeIdentifier.String
          case "numeric"                                            => TypeIdentifier.Float
          case "bool"                                               => TypeIdentifier.Boolean
          case "timestamp"                                          => TypeIdentifier.DateTime
          case "int4"                                               => TypeIdentifier.Int
          case "uuid"                                               => TypeIdentifier.UUID
          case x if x.startsWith("varchar") || x.startsWith("char") => TypeIdentifier.String // mysql
          case "mediumtext"                                         => TypeIdentifier.String // mysql
          case x if x.startsWith("decimal")                         => TypeIdentifier.Float // mysql
          case x if x.startsWith("int")                             => TypeIdentifier.Int // mysql
          case x if x.startsWith("tinyint")                         => TypeIdentifier.Boolean // mysql
          case x if x.startsWith("datetime")                        => TypeIdentifier.DateTime // mysql
          case x                                                    => sys.error(s"Encountered unknown SQL type $x with column ${col.name}. $col")
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

  implicit lazy val introspectedColumnGetResult = GetResult { ps =>
    val dataTypeColumn = db.prismaDialect match {
      case PostgresDialect => "udt_name"
      case MySqlDialect    => "COLUMN_TYPE"
      case x               => sys.error(s"$x is not implemented yet.")
    }
    IntrospectedColumn(
      name = ps.rs.getString("column_name"),
      udtName = ps.rs.getString(dataTypeColumn),
      default = ps.rs.getString("column_default"),
      isNullable = ps.rs.getBoolean("is_nullable"),
      isUnique = ps.rs.getBoolean("is_unique")
    )
  }

  private def getColumns(schema: String, table: String): DBIO[Vector[IntrospectedColumn]] = {
    val dataTypeColumn = db.prismaDialect match {
      case PostgresDialect => "udt_name"
      case MySqlDialect    => "COLUMN_TYPE"
      case x               => sys.error(s"$x is not implemented yet.")
    }
    sql"""
         |SELECT
         |        cols.ordinal_position,
         |        cols.column_name,
         |        cols.#${dataTypeColumn},
         |        cols.column_default,
         |        cols.is_nullable = 'YES' as is_nullable,
         |        false as is_unique
         |      FROM
         |        information_schema.columns AS cols
         |      WHERE
         |        cols.table_schema = $schema
         |        AND cols.table_name  = $table
          """.stripMargin.as[IntrospectedColumn]
  }

  implicit val introspectedForeignKeyGetResult = GetResult { pr =>
    IntrospectedForeignKey(
      name = pr.rs.getString("fkConstraintName"),
      table = pr.rs.getString("fkTableName"),
      column = pr.rs.getString("fkColumnName"),
      referencedTable = pr.rs.getString("referencedTableName"),
      referencedColumn = pr.rs.getString("referencedColumnName")
    )
  }

  private def foreignKeyConstraints(schema: String, table: String): DBIO[Vector[IntrospectedForeignKey]] = {
    if (db.isPostgres) {
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
    } else {
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
           |    kcu.constraint_schema = "MigrationsSpec@default"
           |    AND kcu.referenced_table_name IS NOT NULL;
            """.stripMargin.as[IntrospectedForeignKey]
    }
  }

  implicit val indexGetResult = GetResult { pr =>
    val columns = pr.rs.getArray("column_names").getArray.asInstanceOf[Array[String]]
    Index(
      name = pr.rs.getString("index_name"),
      columns = columns.toVector,
      unique = pr.rs.getBoolean("is_unique")
    )
  }

  private def indexes(schema: String, table: String): DBIO[Vector[Index]] = {
    if (db.isPostgres) {
      sql"""
           |SELECT
           |          tableInfos.relname as table_name,
           |          indexInfos.relname as index_name,
           |          array_agg(columnInfos.attname) as column_names,
           |          rawIndex.indisunique as is_unique,
           |          rawIndex.indisprimary as is_primary_key
           |      FROM
           |          -- pg_class stores infos about tables, indices etc: https://www.postgresql.org/docs/9.3/catalog-pg-class.html
           |          pg_class tableInfos,
           |          pg_class indexInfos,
           |          -- pg_index stores indices: https://www.postgresql.org/docs/9.3/catalog-pg-index.html
           |          pg_index rawIndex,
           |          -- pg_attribute stores infos about columns: https://www.postgresql.org/docs/9.3/catalog-pg-attribute.html
           |          pg_attribute columnInfos,
           |          -- pg_namespace stores info about the schema
           |          pg_namespace schemaInfo
           |      WHERE
           |          -- find table info for index
           |          tableInfos.oid = rawIndex.indrelid
           |          -- find index info
           |          AND indexInfos.oid = rawIndex.indexrelid
           |          -- find table columns
           |          AND columnInfos.attrelid = tableInfos.oid
           |          AND columnInfos.attnum = ANY(rawIndex.indkey)
           |          -- we only consider oridnary tables
           |          AND tableInfos.relkind = 'r'
           |          -- we only consider stuff out of one specific schema
           |          AND tableInfos.relnamespace = schemaInfo.oid
           |      GROUP BY
           |          tableInfos.relname,
           |          indexInfos.relname,
           |          rawIndex.indisunique,
           |          rawIndex.indisprimary
            """.stripMargin.as[Index]
    } else {
      sql"""
           |SELECT
           |        table_name,
           |        index_name,
           |        GROUP_CONCAT(DISTINCT column_name SEPARATOR ', ') AS column_names,
           |        NOT non_unique AS is_unique,
           |        index_name = 'PRIMARY' AS is_primary_key
           |      FROM
           |        information_schema.statistics
           |      WHERE
           |        table_schema = $schema
           |        AND table_name = $table
           |      GROUP BY
           |        table_name, index_name, non_unique
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

  def getSequences(schema: String, table: String): DBIO[Vector[IntrospectedSequence]] = {
    if (db.isPostgres) {
      getSequencesPostgres(schema, table)
    } else if (db.isMySql) {
      getSequencesMySql(schema, table)
    } else {
      sys.error(s"${db.dialect} is not supported here")
    }
  }

  private def getSequencesPostgres(schema: String, table: String): DBIO[Vector[IntrospectedSequence]] = {
    val sequencesForTable = sql"""
                              |select
                              |  cols.column_name, seq.sequence_name, seq.start_value
                              |from
                              |	 information_schema.columns as cols,
                              |	 information_schema.sequences as seq
                              |where
                              |  column_default LIKE '%' || seq.sequence_name || '%' and
                              |  sequence_schema = '#$schema' and
                              |  cols.table_name = '#$table';
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

  private def getSequencesMySql(schema: String, table: String): DBIO[Vector[IntrospectedSequence]] = {
    val sequencesForTable =
      sql"""
           |select column_name
           |from information_schema.COLUMNS
           |where extra = 'auto_increment'
           |and table_name = '#$table'
           |and table_schema = '#$schema';
         """.stripMargin.as[String]
    val currentValueForSequence =
      sql"""
           |select auto_increment
           |from information_schema.TABLES
           |where table_name = '#$table'
           |and table_schema = '#$schema';
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
}
