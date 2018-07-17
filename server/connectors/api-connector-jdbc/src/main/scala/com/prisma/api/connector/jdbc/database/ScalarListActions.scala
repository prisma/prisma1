package com.prisma.api.connector.jdbc.database

import java.sql.PreparedStatement

import com.prisma.api.connector.Filter
import com.prisma.gc_values.{IdGCValue, ListGCValue}
import com.prisma.shared.models.Model
import slick.dbio.DBIOAction
import slick.jdbc.PositionedParameters

trait ScalarListActions extends BuilderBase with FilterConditionBuilder {
  import slickDatabase.profile.api._

  def deleteScalarListValuesByNodeIds(model: Model, ids: Vector[IdGCValue]): DBIO[Unit] = {
    val actions = model.scalarListFields.map { listField =>
      val query = sql
        .deleteFrom(scalarListTable(listField))
        .where(scalarListColumn(listField, "nodeId").in(placeHolders(ids)))

      deleteToDBIO(query)(
        setParams = pp => ids.foreach(pp.setGcValue)
      )
    }
    DBIO.seq(actions: _*)
  }

  def setScalarListValuesByNodeId(model: Model, id: IdGCValue, listFieldMap: Vector[(String, ListGCValue)]) = {
    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, DBIO.successful(Vector(id)))
  }

  def setScalarListValuesByFilter(model: Model, listFieldMap: Vector[(String, ListGCValue)], whereFilter: Option[Filter]) = {
    val condition    = buildConditionForFilter(whereFilter)
    val aliasedTable = modelTable(model).as(topLevelAlias)

    val query = sql
      .select(aliasColumn(model.dbNameOfIdField_!))
      .from(aliasedTable)
      .where(condition)

    val idQuery = queryToDBIO(query)(
      setParams = pp => SetParams.setFilter(pp, whereFilter),
      readResult = rs => rs.readWith(readNodeId(model))
    )

    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, idQuery)
  }

  private def setManyScalarListHelper(model: Model, listFieldMap: Vector[(String, ListGCValue)], idQuery: DBIO[Vector[IdGCValue]]) = {
    import scala.concurrent.ExecutionContext.Implicits.global

    def listInsert(ids: Vector[IdGCValue]) = {
      if (ids.isEmpty) {
        DBIOAction.successful(())
      } else {

        SimpleDBIO[Unit] { x =>
          def valueTuplesForListField(listGCValue: ListGCValue) = {
            for {
              nodeId                   <- ids
              (escapedValue, position) <- listGCValue.values.zip((1 to listGCValue.size).map(_ * 1000))
            } yield {
              (nodeId, position, escapedValue)
            }
          }

          listFieldMap.foreach {
            case (fieldName, listGCValue) =>
              val scalarField = model.getScalarFieldByName_!(fieldName)
              val table       = scalarListTable(scalarField)

              val wipe = sql
                .deleteFrom(table)
                .where(scalarListColumn(scalarField, nodeIdFieldName).in(placeHolders(ids)))
                .getSQL

              val wipeOldValues: PreparedStatement = x.connection.prepareStatement(wipe)
              val pp                               = new PositionedParameters(wipeOldValues)
              ids.foreach(pp.setGcValue)

              wipeOldValues.executeUpdate()

              val insert = sql
                .insertInto(table)
                .columns(
                  scalarListColumn(scalarField, nodeIdFieldName),
                  scalarListColumn(scalarField, positionFieldName),
                  scalarListColumn(scalarField, valueFieldName)
                )
                .values(placeHolder, placeHolder, placeHolder)
                .getSQL

              val insertNewValues: PreparedStatement = x.connection.prepareStatement(insert)
              val newValueTuples                     = valueTuplesForListField(listGCValue)

              newValueTuples.foreach { tuple =>
                insertNewValues.setGcValue(1, tuple._1)
                insertNewValues.setInt(2, tuple._2)
                insertNewValues.setGcValue(3, tuple._3)
                insertNewValues.addBatch()
              }
              insertNewValues.executeBatch()
          }
        }
      }
    }

    for {
      nodeIds <- idQuery
      action  <- listInsert(nodeIds)
    } yield action
  }
}
