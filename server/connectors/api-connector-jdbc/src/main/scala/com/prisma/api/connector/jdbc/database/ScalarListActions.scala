package com.prisma.api.connector.jdbc.database

import java.sql.PreparedStatement

import com.prisma.api.connector.Filter
import com.prisma.gc_values.{IdGCValue, ListGCValue}
import com.prisma.shared.models.{Model, ScalarField}
import slick.dbio.DBIOAction

trait ScalarListActions extends BuilderBase with FilterConditionBuilder with NodeManyQueries {
  import slickDatabase.profile.api._

  def createScalarListValuesForNodeId(model: Model, id: IdGCValue, listFieldMap: Vector[(String, ListGCValue)]): DBIO[Unit] = {
    val actions = listFieldMap.map {
      case (fieldName, listGCValue) =>
        val scalarField = model.getScalarFieldByName_!(fieldName)
        insertListValueForIds(scalarField, listGCValue, Vector(id))
    }
    DBIO.seq(actions: _*)
  }

  def updateScalarListValuesForNodeId(model: Model, id: IdGCValue, listFieldMap: Vector[(String, ListGCValue)]): DBIO[Unit] = {
    updateScalarListValuesForIds(model, listFieldMap, Vector(id))
  }

  def deleteScalarListValuesByNodeIds(model: Model, ids: Vector[IdGCValue]): DBIO[Unit] = {
    val actions = model.scalarListFields.map(deleteListValuesForIds(_, ids))
    DBIO.seq(actions: _*)
  }

  def updateScalarListValuesForIds(model: Model, listFieldMap: Vector[(String, ListGCValue)], ids: Vector[IdGCValue]): DBIO[Unit] = {
    if (ids.isEmpty || listFieldMap.isEmpty) {
      DBIOAction.successful(())
    } else {
      val actions = listFieldMap.map {
        case (fieldName, listGCValue) =>
          val scalarField = model.getScalarFieldByName_!(fieldName)
          DBIO.seq(
            deleteListValuesForIds(scalarField, ids),
            insertListValueForIds(scalarField, listGCValue, ids)
          )
      }
      DBIO.seq(actions: _*)
    }
  }

  private def deleteListValuesForIds(listField: ScalarField, ids: Vector[IdGCValue]): DBIO[Unit] = {
    val query = sql
      .deleteFrom(scalarListTable(listField))
      .where(scalarListColumn(listField, nodeIdFieldName).in(placeHolders(ids)))

    deleteToDBIO(query)(
      setParams = pp => ids.foreach(pp.setGcValue)
    )
  }

  private def insertListValueForIds(scalarField: ScalarField, value: ListGCValue, ids: Vector[IdGCValue]) = SimpleDBIO[Unit] { ctx =>
    val table = scalarListTable(scalarField)
    val insert = sql
      .insertInto(table)
      .columns(
        scalarListColumn(scalarField, nodeIdFieldName),
        scalarListColumn(scalarField, positionFieldName),
        scalarListColumn(scalarField, valueFieldName)
      )
      .values(placeHolder, placeHolder, placeHolder)
      .getSQL

    val insertNewValues: PreparedStatement = ctx.connection.prepareStatement(insert)
    val newValueTuples                     = valueTuplesForListField(ids, value)

    newValueTuples.foreach { tuple =>
      insertNewValues.setGcValue(1, tuple._1)
      insertNewValues.setInt(2, tuple._2)
      insertNewValues.setGcValue(3, tuple._3)
      insertNewValues.addBatch()
    }
    insertNewValues.executeBatch()
  }

  private def valueTuplesForListField(ids: Vector[IdGCValue], listGCValue: ListGCValue) = {
    for {
      nodeId            <- ids
      (value, position) <- listGCValue.values.zip((1 to listGCValue.size).map(_ * 1000))
    } yield {
      (nodeId, position, value)
    }
  }
}
