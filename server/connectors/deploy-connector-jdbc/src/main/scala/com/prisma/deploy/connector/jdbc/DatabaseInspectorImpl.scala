package com.prisma.deploy.connector.jdbc

import java.sql.Types

import com.prisma.deploy.connector._
import com.prisma.shared.models.TypeIdentifier
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.{MColumn, MForeignKey, MIndexInfo, MTable}

import scala.concurrent.{ExecutionContext, Future}

case class DatabaseInspectorImpl(db: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext) extends DatabaseInspector {

  override def inspect(schema: String): Future[Tables] = db.run(action(schema))

  def action(schema: String): DBIO[Tables] = {
    for {
      // the line below does not work perfectly on postgres. E.g. it will return tables for schemas "passive_test" and "passive$test" when param is "passive_test"
      // we therefore have one additional filter step
      potentialTables <- MTable.getTables(cat = None, schemaPattern = None, namePattern = None, types = None)
      mTables         = potentialTables.filter(table => table.name.schema.orElse(table.name.catalog).contains(schema))
      tables          <- DBIO.sequence(mTables.map(mTableToModel))
    } yield {
      Tables(tables)
    }
  }

  def mTableToModel(mTable: MTable): DBIO[Table] = {
    for {
      mColumns     <- mTable.getColumns
      importedKeys <- mTable.getImportedKeys
      mIndexes     <- mTable.getIndexInfo()
    } yield {
      val columns = mColumns.map { mColumn =>
        val importedKeyForColumn = importedKeys.find(_.fkColumn == mColumn.name)
        mColumnToModel(mColumn, importedKeyForColumn)
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

  def mColumnToModel(mColumn: MColumn, mForeignKey: Option[MForeignKey]): Column = {
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
      foreignKey = mForeignKey.map(mfk => ForeignKey(mfk.pkTable.name, mfk.pkColumn)),
      isRequired = isRequired
    )
  }
}
