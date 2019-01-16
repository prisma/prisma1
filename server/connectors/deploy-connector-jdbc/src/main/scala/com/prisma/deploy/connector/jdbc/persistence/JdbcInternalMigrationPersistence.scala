package com.prisma.deploy.connector.jdbc.persistence
import java.sql.{ResultSet, Timestamp}
import java.util.Calendar

import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.jdbc.JdbcBase
import com.prisma.deploy.connector.persistence.{InternalMigration, InternalMigrationPersistence}
import com.prisma.shared.models.Project
import org.jooq.impl.DSL.{exists, field, name, table}
import slick.dbio.DBIO

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

case class JdbcInternalMigrationPersistence(slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcBase with InternalMigrationPersistence {
  val theTable        = table(name("InternalMigration"))
  val idColumn        = field(name("id"))
  val appliedAtColumn = field(name("appliedAt"))

  override def create(mig: InternalMigration): Future[Unit] = {
    val createQuery = insertToDBIO {
      sql
        .insertInto(theTable)
        .columns(idColumn, appliedAtColumn)
        .values(placeHolder, placeHolder)
    }(setParams = { pp =>
      pp.setString(mig.id)
      pp.setTimestamp(new Timestamp(Calendar.getInstance().getTime().getTime()))
    })

    val action = for {
      migrationExists <- migrationExists(mig.id)
      _               <- if (migrationExists) DBIO.successful(()) else createQuery
    } yield ()
    database.run(action)
  }

  private def migrationExists(id: String): DBIO[Boolean] = {
    val query = sql
      .select(idColumn)
      .from(theTable)
      .where(idColumn.equal(placeHolder))

    queryToDBIO(query)(
      setParams = { pp =>
        pp.setString(id)
      },
      readResult = { rs =>
        rs.next()
      }
    )
  }

  override def loadAll(): Future[Vector[InternalMigration]] = {
    val query = sql
      .select(idColumn)
      .from(theTable)

    val action = queryToDBIO(query)(
      readResult = { rs =>
        val buffer = ArrayBuffer.empty[InternalMigration]
        while (rs.next()) {
          buffer += internalMigrationFromResultSet(rs)
        }

        buffer.toVector
      }
    )
    database.run(action)
  }

  private def internalMigrationFromResultSet(rs: ResultSet): InternalMigration = {
    InternalMigration(id = rs.getString(idColumn.getName))
  }

}
