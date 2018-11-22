package com.prisma.deploy.connector.jdbc

import com.prisma.deploy.connector._
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable

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
      mColumns <- mTable.getColumns
//      importedKeys <- mTable.getImportedKeys
    } yield {
      val columns = mColumns.map(mc => Column(mc.name, mc.typeName, foreignKey = None))
      val indexes = Vector.empty[Index]
      Table(mTable.name.name, columns, indexes)
    }
  }
}
