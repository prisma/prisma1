package com.prisma.deploy.connector.jdbc

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector._
import com.prisma.shared.models.TypeIdentifier
import slick.dbio.DBIO
import slick.jdbc.GetResult
import slick.jdbc.meta.{MColumn, MForeignKey, MIndexInfo, MTable}

import scala.concurrent.{ExecutionContext, Future}

case class DatabaseInspectorImpl(db: SlickDatabase)(implicit ec: ExecutionContext) extends DatabaseInspector {
  import db.profile.api.actionBasedSQLInterpolation

  override def inspect(schema: String): Future[DatabaseSchema] = db.database.run(action(schema))

  def action(schema: String): DBIO[DatabaseSchema] = {
//    for {
//      // we filter on catalog and schema at the same time, because:
//      // 1. For MySQL only catalog will be populated.
//      // 2. For Postgres only schema will be populated.
//      // 3. It works when both are populated.
//      potentialTables <- MTable.getTables(cat = Some(schema), schemaPattern = Some(schema), namePattern = None, types = None)
//      // the line above does not work perfectly on postgres. E.g. it will return tables for schemas "passive_test" and "passive$test" when param is "passive_test"
//      // we therefore have one additional filter step in memory
//      mTables = potentialTables.filter(table => table.name.schema.orElse(table.name.catalog).contains(schema))
//      tables  <- DBIO.sequence(mTables.map(mTableToModel))
//    } yield {
//      DatabaseSchema(tables)
//    }
    for {
      tableNames <- getTableNames(schema)
//      _          = println(s"tableNames: $tableNames")
      tables <- DBIO.sequence(tableNames.map(name => getTable(schema, name)))
    } yield {
      DatabaseSchema(tables)
    }
  }

  private def getTable(schema: String, table: String): DBIO[Table] = {
    for {
      introspectedColumns <- getColumns(schema, table)
//      _                       = println(s"getTable: $schema, $table")
      introspectedForeignKeys <- foreignKeyConstraints(schema, table)
//      _                       = println(introspectedForeignKeys)
    } yield {
      val columns = introspectedColumns.map { col =>
        // this needs to be extended further in the future if we support arbitrary SQL types
        import java.sql.Types._
        val typeIdentifier = col.udtName match {
          case "varchar" | "string" | "text" | "bpchar" => TypeIdentifier.String
          case "numeric"                                => TypeIdentifier.Float
          case "bool"                                   => TypeIdentifier.Boolean
          case "timestamp"                              => TypeIdentifier.DateTime
          case "int4"                                   => TypeIdentifier.Int
          case "uuid"                                   => TypeIdentifier.UUID
//          case VARCHAR | CHAR | LONGVARCHAR        => TypeIdentifier.String
//          case FLOAT | NUMERIC | DECIMAL           => TypeIdentifier.Float
//          case BOOLEAN | BIT                       => TypeIdentifier.Boolean
//          case TIMESTAMP                           => TypeIdentifier.DateTime
//          case INTEGER                             => TypeIdentifier.Int
//          case OTHER if mColumn.typeName == "uuid" => TypeIdentifier.UUID
          case x => sys.error(s"Encountered unknown SQL type $x with column ${col.name}. $col")
        }
        val fk = introspectedForeignKeys.find(fk => fk.column == col.name).map { fk =>
          ForeignKey(fk.referencedTable, fk.referencedColumn)
        }
        Column(
          name = col.name,
          tpe = col.udtName,
          typeIdentifier = typeIdentifier,
          isRequired = !col.isNullable,
          foreignKey = fk,
          sequence = None
        )(_)
      }
      Table(table, columns, indexes = Vector.empty)
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

  case class IntrospectedColumn(name: String, udtName: String, isUpdatable: Boolean, default: String, isNullable: Boolean, isUnique: Boolean)

  implicit lazy val introspectedColumnGetResult = GetResult { ps =>
    IntrospectedColumn(
      name = ps.rs.getString("column_name"),
      udtName = ps.rs.getString("udt_name"),
      isUpdatable = ps.rs.getBoolean("is_updatable"),
      default = ps.rs.getString("column_default"),
      isNullable = ps.rs.getBoolean("is_nullable"),
      isUnique = ps.rs.getBoolean("is_unique")
    )
  }

  private def getColumns(schema: String, table: String): DBIO[Vector[IntrospectedColumn]] = {
    sql"""
         |SELECT
         |        cols.ordinal_position,
         |        cols.column_name,
         |        cols.udt_name,
         |        cols.is_updatable,
         |        cols.column_default,
         |        cols.is_nullable = 'YES' as is_nullable,
         |        EXISTS(
         |          SELECT * FROM
         |            information_schema.constraint_column_usage columnConstraint
         |          LEFT JOIN
         |            information_schema.table_constraints tableConstraints
         |          ON
         |            columnConstraint.constraint_name = tableConstraints.constraint_name
         |          WHERE
         |            cols.column_name = columnConstraint.column_name
         |            AND cols.table_name = columnConstraint.table_name
         |            AND cols.table_schema = columnConstraint.table_schema
         |            AND tableConstraints.constraint_type = 'UNIQUE'
         |          ) AS is_unique
         |      FROM
         |        information_schema.columns AS cols
         |      WHERE
         |        cols.table_schema = $schema
         |        AND cols.table_name  = $table
          """.stripMargin.as[IntrospectedColumn]
  }

  case class IntrospectedForeignKey(name: String, table: String, column: String, referencedTable: String, referencedColumn: String)

  implicit val introspectedForeignKeyGetResult = GetResult { ps =>
    IntrospectedForeignKey(
      name = ps.rs.getString("fkConstraintName"),
      table = ps.rs.getString("fkTableName"),
      column = ps.rs.getString("fkColumnName"),
      referencedTable = ps.rs.getString("referencedTableName"),
      referencedColumn = ps.rs.getString("referencedColumnName")
    )
  }

  private def foreignKeyConstraints(schema: String, table: String): DBIO[Vector[IntrospectedForeignKey]] = {
    sql"""
         |SELECT
         |	kcu.constraint_name as "fkConstraintName",
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

  def mTableToModel(mTable: MTable): DBIO[Table] = {
    for {
      mColumns     <- mTable.getColumns
      importedKeys <- mTable.getImportedKeys
      mIndexes     <- mTable.getIndexInfo()
      sequences    <- getSequences(mTable.name.schema.orElse(mTable.name.catalog).get, mTable.name.name)
    } yield {
      val columns = mColumns.map { mColumn =>
        val importedKeyForColumn = importedKeys.find(_.fkColumn == mColumn.name)
        mColumnToModel(mColumn, importedKeyForColumn, sequences)
      }
      val indexes = mIndexesToModels(mIndexes)
      Table(mTable.name.name, columns, indexes)
    }
  }

  def mIndexesToModels(mIndex: Vector[MIndexInfo]): Vector[Index] = {
    val byName = mIndex.groupBy(_.indexName)
    byName.map {
      case (Some(indexName), mIndexes) =>
        Index(
          name = indexName,
          columns = mIndexes.flatMap(_.column),
          unique = !mIndexes.head.nonUnique
        )
      case (None, _) =>
        sys.error("There must always be an index name")
    }.toVector
  }

  case class MSequence(column: String, name: String, current: Int)

  def getSequences(schema: String, table: String): DBIO[Vector[MSequence]] = {
    if (db.isPostgres) {
      getSequencesPostgres(schema, table)
    } else if (db.isMySql) {
      getSequencesMySql(schema, table)
    } else {
      sys.error(s"${db.dialect} is not supported here")
    }
  }

  private def getSequencesPostgres(schema: String, table: String): DBIO[Vector[MSequence]] = {
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
          MSequence(column, sequence, current)
      }
    }
    action.withPinnedSession
  }

  private def getSequencesMySql(schema: String, table: String): DBIO[Vector[MSequence]] = {
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
      } yield MSequence(column = column, name = "sequences_are_not_named_in_mysql", current = currentValue)
      x.toVector
    }
  }

  //Fixme needs to handle sqlite

  def mColumnToModel(mColumn: MColumn, mForeignKey: Option[MForeignKey], mSequences: Vector[MSequence]): Table => Column = {
    val isRequired = !mColumn.nullable.getOrElse(true) // sometimes the metadata can't definitely say if something is nullable. We treat those as not required.
    // this needs to be extended further in the future if we support arbitrary SQL types
    import java.sql.Types._
    val typeIdentifier = mColumn.sqlType match {
      case VARCHAR | CHAR | LONGVARCHAR        => TypeIdentifier.String
      case FLOAT | NUMERIC | DECIMAL           => TypeIdentifier.Float
      case BOOLEAN | BIT                       => TypeIdentifier.Boolean
      case TIMESTAMP                           => TypeIdentifier.DateTime
      case INTEGER                             => TypeIdentifier.Int
      case OTHER if mColumn.typeName == "uuid" => TypeIdentifier.UUID
      case x                                   => sys.error(s"Encountered unknown SQL type $x with column ${mColumn.name}. $mColumn")
    }
    Column(
      name = mColumn.name,
      tpe = mColumn.typeName,
      typeIdentifier = typeIdentifier,
      isRequired = isRequired,
      foreignKey = mForeignKey.map(mfk => ForeignKey(mfk.pkTable.name, mfk.pkColumn)),
      sequence = mSequences.find(_.column == mColumn.name).map(mseq => Sequence(mseq.name, mseq.current))
    )
  }
}
