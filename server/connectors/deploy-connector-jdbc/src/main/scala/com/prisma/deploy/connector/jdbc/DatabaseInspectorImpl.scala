package com.prisma.deploy.connector.jdbc

import java.sql.Types

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector._
import com.prisma.shared.models.TypeIdentifier
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.{MColumn, MForeignKey, MIndexInfo, MTable}

import scala.concurrent.{ExecutionContext, Future}

case class DatabaseInspectorImpl(db: SlickDatabase)(implicit ec: ExecutionContext) extends DatabaseInspector {
  import db.profile.api.actionBasedSQLInterpolation

  override def inspect(schema: String): Future[DatabaseSchema] = db.database.run(action(schema))

  def action(schema: String): DBIO[DatabaseSchema] = {
    for {
      // the line below does not work perfectly on postgres. E.g. it will return tables for schemas "passive_test" and "passive$test" when param is "passive_test"
      // we therefore have one additional filter step
      potentialTables <- MTable.getTables(cat = None, schemaPattern = None, namePattern = None, types = None)
      mTables         = potentialTables.filter(table => table.name.schema.orElse(table.name.catalog).contains(schema))
      tables          <- DBIO.sequence(mTables.map(mTableToModel))
    } yield {
      DatabaseSchema(tables)
    }
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
