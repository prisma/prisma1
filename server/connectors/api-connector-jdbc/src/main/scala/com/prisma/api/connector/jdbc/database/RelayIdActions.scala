package com.prisma.api.connector.jdbc.database

import com.prisma.gc_values.{IdGCValue, UuidGCValue}
import com.prisma.shared.models.Model
import com.prisma.shared.models.TypeIdentifier.Cuid

trait RelayIdActions extends BuilderBase {
  import slickDatabase.profile.api._

  def createRelayId(model: Model, id: IdGCValue): DBIO[_] = {
    lazy val query = sql
      .insertInto(relayTable)
      .columns(relayIdColumn, relayStableIdentifierColumn)
      .values(placeHolder, placeHolder)

    insertToDBIO(query)(
      setParams = pp => {
        id match {
          case UuidGCValue(uid) => pp.setString(uid.toString)
          case x                => pp.setGcValue(x)
        }

        pp.setString(model.stableIdentifier)
      }
    )
  }

  def deleteRelayIds(ids: Vector[IdGCValue]): DBIO[Unit] = {
    val query = sql
      .deleteFrom(relayTable)
      .where(relayIdColumn.in(placeHolders(ids)))

    deleteToDBIO(query)(
      setParams = pp =>
        ids.foreach {
          case UuidGCValue(uuid) => pp.setString(uuid.toString) // the id column in the relay table is a string, setting a uuid with .setObject() would blow up
          case id                => pp.setGcValue(id)
      }
    )
  }
}
