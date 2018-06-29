package com.prisma.api.connector.jdbc.database

import java.sql.PreparedStatement

import com.prisma.api.connector.Filter
import com.prisma.gc_values.{IdGCValue, ListGCValue}
import com.prisma.shared.models.Model
import org.jooq.impl.DSL.{field, name, table}
import slick.dbio.DBIOAction
import slick.jdbc.PositionedParameters

trait ScalarListActions extends BuilderBase with FilterConditionBuilder {
  import slickDatabase.profile.api._

  def deleteScalarListEntriesByIds(model: Model, ids: Vector[IdGCValue]): DBIO[Unit] = {

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

  def setScalarListById(model: Model, id: IdGCValue, listFieldMap: Vector[(String, ListGCValue)]) = {
    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, DBIO.successful(Vector(id)))
  }

  def setManyScalarLists(model: Model, listFieldMap: Vector[(String, ListGCValue)], whereFilter: Option[Filter]) = {
    val idQuery = SimpleDBIO { ctx =>
      val condition    = buildConditionForFilter(whereFilter)
      val aliasedTable = modelTable(model).as(topLevelAlias)

      val queryString = sql
        .select(aliasColumn(model.dbNameOfIdField_!))
        .from(aliasedTable)
        .where(condition)
        .getSQL

      val ps = ctx.connection.prepareStatement(queryString)
      SetParams.setFilter(new PositionedParameters(ps), whereFilter)
      val rs = ps.executeQuery()
      rs.as(readNodeId(model))
    }

    if (listFieldMap.isEmpty) DBIOAction.successful(()) else setManyScalarListHelper(model, listFieldMap, idQuery)
  }

  def setManyScalarListHelper(model: Model, listFieldMap: Vector[(String, ListGCValue)], idQuery: DBIO[Vector[IdGCValue]]) = {
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
              val dbNameOfField = model.getFieldByName_!(fieldName).dbName
              val tableName     = s"${model.dbName}_$dbNameOfField"

              val condition = ids.length match {
                case 1 => field(name(schemaName, tableName, nodeIdFieldName)).equal(placeHolder)
                case _ => field(name(schemaName, tableName, nodeIdFieldName)).in(ids.map(_ => placeHolder): _*)
              }

              val wipe = sql
                .deleteFrom(table(name(schemaName, tableName)))
                .where(condition)
                .getSQL

              val wipeOldValues: PreparedStatement = x.connection.prepareStatement(wipe)
              ids.zipWithIndex.foreach { zip =>
                wipeOldValues.setGcValue(zip._2 + 1, zip._1)
              }

              wipeOldValues.executeUpdate()

              val insert = sql
                .insertInto(table(name(schemaName, tableName)))
                .columns(
                  field(name(schemaName, tableName, nodeIdFieldName)),
                  field(name(schemaName, tableName, positionFieldName)),
                  field(name(schemaName, tableName, valueFieldName))
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
