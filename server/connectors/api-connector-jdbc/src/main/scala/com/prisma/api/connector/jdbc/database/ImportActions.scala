package com.prisma.api.connector.jdbc.database

import java.sql.{PreparedStatement, Statement}

import com.prisma.api.connector._
import com.prisma.connector.shared.jdbc.SharedJdbcExtensions
import com.prisma.gc_values._
import com.prisma.shared.models.ScalarField
import com.prisma.slick.ReadsResultSet
import cool.graph.cuid.Cuid
import org.jooq.impl.DSL.max

import scala.concurrent.ExecutionContext

trait ImportActions extends BuilderBase with SharedJdbcExtensions {
  import slickDatabase.profile.api._

  def importNodes(mutaction: ImportNodes): SimpleDBIO[Vector[String]] = {

    SimpleDBIO[Vector[String]] { jdbcActionContext =>
      val model         = mutaction.model
      val argsWithIndex = mutaction.args.zipWithIndex

      val nodeResult: Vector[String] = try {
        val fields = model.scalarNonListFields.map(modelColumn).toVector

        val query = sql
          .insertInto(modelTable(model))
          .columns(fields: _*)
          .values(placeHolders(fields))

        val itemInsert: PreparedStatement = jdbcActionContext.connection.prepareStatement(query.getSQL)
        val currentTimeStamp              = currentSqlTimestampUTC

        mutaction.args.foreach { arg =>
          model.scalarNonListFields.zipWithIndex.foreach {
            case (field, index) =>
              arg.rootGCMap.get(field.name) match {
                case Some(NullGCValue) if field.isCreatedAt || field.isUpdatedAt => itemInsert.setTimestamp(index + 1, currentTimeStamp)
                case Some(gCValue)                                               => itemInsert.setGcValue(index + 1, gCValue)
                case None if field.isCreatedAt || field.isUpdatedAt              => itemInsert.setTimestamp(index + 1, currentTimeStamp)
                case None                                                        => itemInsert.setNull(index + 1, java.sql.Types.NULL)
              }
          }
          itemInsert.addBatch()
        }

        itemInsert.executeBatch()

        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          e.getUpdateCounts.zipWithIndex
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedId = argsWithIndex.find(_._2 == failed._2).get._1.rootGC.idFieldByName(mutaction.model.idField_!.name).value
              s"Failure inserting ${model.dbName} with Id: $failedId. Cause: ${removeConnectionInfoFromCause(e.getCause())}"
            }
            .toVector

        case e: Exception =>
          Vector(s"Failure inserting ${model.dbName}. Cause: ${e.getCause.toString}")
      }

      if (nodeResult.nonEmpty) throw new Exception(nodeResult.mkString("-@-"))
      nodeResult
    }
  }

  private def removeConnectionInfoFromCause(cause: Throwable): String = {
    if (cause == null) {
      "unknown"
    } else {
      val stringified              = cause.toString
      val connectionSubStringStart = stringified.indexOf(": ERROR:")
      stringified.substring(connectionSubStringStart + 9)
    }
  }

  def importRelations(mutaction: ImportRelations): SimpleDBIO[Vector[String]] = {
    val argsWithIndex: Seq[((IdGCValue, IdGCValue), Int)] = mutaction.args.zipWithIndex
    val relation                                          = mutaction.relation

    SimpleDBIO[Vector[String]] { x =>
      val res = try {
        if (relation.relationTableHas3Columns) {
          val query = sql
            .insertInto(relationTable(relation))
            .columns(
              relationIdColumn(relation),
              relationColumn(relation, relation.modelAField.relationSide),
              relationColumn(relation, relation.modelBField.relationSide)
            )
            .values(placeHolder, placeHolder, placeHolder)

          val relationInsert: PreparedStatement = x.connection.prepareStatement(query.getSQL)
          mutaction.args.foreach { arg =>
            relationInsert.setString(1, Cuid.createCuid())
            relationInsert.setGcValue(2, arg._1)
            relationInsert.setGcValue(3, arg._2)
            relationInsert.addBatch()
          }
          relationInsert.executeBatch()

        } else if (relation.isRelationTable) {
          val query = sql
            .insertInto(relationTable(relation))
            .columns(
              relationColumn(relation, relation.modelAField.relationSide),
              relationColumn(relation, relation.modelBField.relationSide)
            )
            .values(placeHolder, placeHolder)

          val relationInsert: PreparedStatement = x.connection.prepareStatement(query.getSQL)
          mutaction.args.foreach { arg =>
            relationInsert.setGcValue(1, arg._1)
            relationInsert.setGcValue(2, arg._2)
            relationInsert.addBatch()
          }
          relationInsert.executeBatch()
        } else if (relation.isInlineRelation) {
          val referencingColumn    = relation.inlineManifestation.get.referencingColumn
          val isInlinedInModelA    = relation.modelAField.relationIsInlinedInParent
          val rowToUpdateCondition = if (isInlinedInModelA) idField(relation.modelA).equal(placeHolder) else idField(relation.modelB).equal(placeHolder)

          val query = sql
            .update(relationTable(relation))
            .setColumnsWithPlaceHolders(Vector(referencingColumn))
            .where(rowToUpdateCondition)

          val relationInsert: PreparedStatement = x.connection.prepareStatement(query.getSQL)
          mutaction.args.foreach { arg =>
            if (isInlinedInModelA) {
              relationInsert.setGcValue(1, arg._2)
              relationInsert.setGcValue(2, arg._1)
            } else {
              relationInsert.setGcValue(1, arg._1)
              relationInsert.setGcValue(2, arg._2)
            }
            relationInsert.addBatch()
          }
          relationInsert.executeBatch()
        }
        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          val faileds = e.getUpdateCounts.zipWithIndex

          faileds
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedA = argsWithIndex.find(_._2 == failed._2).get._1._1
              val failedB = argsWithIndex.find(_._2 == failed._2).get._1._2
              s"Failure inserting into relationtable ${relation.relationTableName} with ids $failedA and $failedB. Cause: ${removeConnectionInfoFromCause(e.getCause())}"
            }
            .toVector

        case e: Exception =>
          Vector(s"Failure inserting into relationtable ${relation.relationTableName}. Cause: ${e.getMessage}")
      }

      if (res.nonEmpty) throw new Exception(res.mkString("-@-"))
      res
    }
  }

  def importScalarLists(mutaction: ImportScalarLists)(implicit ec: ExecutionContext) = {
    val field   = mutaction.field
    val nodeIds = mutaction.values.keys

    for {
      startPositions: Map[IdGCValue, Int] <- startPositions(field, nodeIds.toSeq)
      // begin massage

      listValuesWithStartPosition: Iterable[(IdGCValue, ListGCValue, Int)] = {
        mutaction.values.map {
          case (id, listValue) => (id, listValue, startPositions.getOrElse(id, 0))
        }
      }

      individualValuesWithPosition: Iterable[(IdGCValue, GCValue, Int)] = listValuesWithStartPosition.flatMap {
        case (id, list, start) =>
          list.values.zipWithIndex.map { case (value, index) => (id, value, start + (index * 1000) + 1000) }
      }
      // end massage
      res <- importScalarListValues(field, individualValuesWithPosition)
    } yield res
  }

  private def startPositions(field: ScalarField, nodeIds: Seq[IdGCValue]): DBIO[Map[IdGCValue, Int]] = {
    val nodeIdField  = scalarListColumn(field, nodeIdFieldName)
    val placeholders = nodeIds.map(_ => placeHolder)

    val query = sql
      .select(nodeIdField, max(scalarListColumn(field, positionFieldName)).as("max"))
      .from(scalarListTable(field))
      .groupBy(nodeIdField)
      .having(nodeIdField.in(placeholders: _*))

    val reads = ReadsResultSet { rs =>
      val nodeId = rs.getId(field.model, nodeIdFieldName)
      val start  = rs.getInt("max")
      (nodeId, start)
    }

    queryToDBIO(query)(
      setParams = pp => nodeIds.foreach(pp.setGcValue),
      readResult = rs => rs.readWith(reads).toMap
    )
  }

  private def importScalarListValues(field: ScalarField, values: Iterable[(IdGCValue, GCValue, Int)]): DBIO[Vector[String]] = SimpleDBIO { ctx =>
    val argsWithIndex: Iterable[((IdGCValue, GCValue, Int), Int)] = values.zipWithIndex

    val res = try {
      val query = sql
        .insertInto(scalarListTable(field))
        .columns(scalarListColumn(field, nodeIdFieldName), scalarListColumn(field, valueFieldName), scalarListColumn(field, positionFieldName))
        .values(placeHolder, placeHolder, placeHolder)

      val ps: PreparedStatement = ctx.connection.prepareStatement(query.getSQL)
      // do it
      values.foreach { value =>
        ps.setGcValue(1, value._1)
        ps.setGcValue(2, value._2)
        ps.setInt(3, value._3)
        ps.addBatch()
      }
      ps.executeBatch()
      Vector.empty
    } catch {
      case e: java.sql.BatchUpdateException =>
        e.getUpdateCounts.zipWithIndex
          .filter(element => element._1 == Statement.EXECUTE_FAILED)
          .map { failed =>
            val failedValue = argsWithIndex.find(_._2 == failed._2).get._1._2
            val failedId    = argsWithIndex.find(_._2 == failed._2).get._1._1

            s"Failure inserting into listTable ${field.model.dbName}_${field.dbName} for the id ${failedId.value} for value ${failedValue.value}. Cause: ${removeConnectionInfoFromCause(e.getCause)}"
          }
          .toVector

      case e: Exception =>
        Vector(s"Failure inserting into listTable ${field.model.dbName}_${field.dbName}: Cause:${e.getMessage}")
    }

    if (res.nonEmpty) throw new Exception(res.mkString("-@-"))
    res
  }
}
