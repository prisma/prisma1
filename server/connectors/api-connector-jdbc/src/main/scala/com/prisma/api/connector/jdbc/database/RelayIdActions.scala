package com.prisma.api.connector.jdbc.database

import java.sql.{PreparedStatement, Statement}

import com.prisma.gc_values.IdGCValue
import com.prisma.shared.models.Model

trait RelayIdActions extends BuilderBase {
  import slickDatabase.profile.api._

  def createRelayRowById(model: Model, id: IdGCValue): DBIO[_] = {
    SimpleDBIO[Boolean] { x =>
      lazy val queryString: String = {
        sql
          .insertInto(relayTable)
          .columns(relayIdColumn, relayStableIdentifierColumn)
          .values(placeHolder, placeHolder)
          .getSQL
      }

      val statement: PreparedStatement = x.connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS)
      statement.setGcValue(1, id)
      statement.setString(2, model.stableIdentifier)

      statement.execute()
    }
  }

  def deleteRelayRowsByIds(ids: Vector[IdGCValue]): DBIO[Unit] = {
    val query = sql
      .deleteFrom(relayTable)
      .where(relayIdColumn.in(placeHolders(ids)))

    deleteToDBIO(query)(
      setParams = pp => ids.foreach(pp.setGcValue)
    )
  }
}
