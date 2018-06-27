package com.prisma.api.connector.postgresql.database

import java.sql.{PreparedStatement, Statement}

import com.prisma.api.connector.postgresql.database.JooqQueryBuilders.placeHolder
import com.prisma.api.connector.{CreateDataItemsImport, CreateRelationRowsImport, PushScalarListsImport}
import com.prisma.gc_values.{GCValue, IdGCValue, ListGCValue, NullGCValue}
import com.prisma.shared.models.ScalarField
import cool.graph.cuid.Cuid
import org.jooq.impl.DSL.max

import scala.concurrent.ExecutionContext

trait ImportActions extends BuilderBase {

  import com.prisma.api.connector.postgresql.database.JdbcExtensions._
  import com.prisma.api.connector.postgresql.database.PostgresSlickExtensions._
  import com.prisma.slick.NewJdbcExtensions._
  import slickDatabase.profile.api._

  def createDataItemsImport(mutaction: CreateDataItemsImport): SimpleDBIO[Vector[String]] = {

    SimpleDBIO[Vector[String]] { jdbcActionContext =>
      val model         = mutaction.model
      val argsWithIndex = mutaction.args.zipWithIndex

      val nodeResult: Vector[String] = try {
        val fields = model.scalarNonListFields.map(modelColumn(model, _)).toVector

        val query = sql
          .insertInto(modelTable(model))
          .columns(fields: _*)
          .values(placeHolders(fields))

        val itemInsert: PreparedStatement = jdbcActionContext.connection.prepareStatement(query.getSQL)
        val currentTimeStamp              = currentTimeStampUTC

        //Fixme have a helper for adding updatedAt / createdAt
        mutaction.args.foreach { arg =>
          val argsAsRoot = arg.raw.asRoot
          model.scalarNonListFields.zipWithIndex.foreach {
            case (field, index) =>
              argsAsRoot.map.get(field.name) match {
                case Some(NullGCValue) if field.name == "createdAt" || field.name == "updatedAt" => itemInsert.setTimestamp(index + 1, currentTimeStamp)
                case Some(gCValue)                                                               => itemInsert.setGcValue(index + 1, gCValue)
                case None if field.name == "createdAt" || field.name == "updatedAt"              => itemInsert.setTimestamp(index + 1, currentTimeStamp)
                case None                                                                        => itemInsert.setNull(index + 1, java.sql.Types.NULL)
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
              val failedId = argsWithIndex.find(_._2 == failed._2).get._1.raw.asRoot.idField.value
              s"Failure inserting ${model.dbName} with Id: $failedId. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector
        case e: Exception => Vector(e.getCause.toString)
      }

      val relayResult: Vector[String] = try {
        val query = sql
          .insertInto(relayTable)
          .columns(relayIdColumn, relayStableIdentifierColumn)
          .values(placeHolder, placeHolder)

        val relayInsert: PreparedStatement = jdbcActionContext.connection.prepareStatement(query.getSQL)

        mutaction.args.foreach { arg =>
          relayInsert.setGcValue(1, arg.raw.asRoot.idField)
          relayInsert.setString(2, model.stableIdentifier)
          relayInsert.addBatch()
        }
        relayInsert.executeBatch()

        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          e.getUpdateCounts.zipWithIndex
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedId = argsWithIndex.find(_._2 == failed._2).get._1.raw.asRoot.idField.value
              s"Failure inserting RelayRow with Id: $failedId. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector
        case e: Exception => Vector(e.getMessage)
      }

      val res = nodeResult ++ relayResult
      if (res.nonEmpty) throw new Exception(res.mkString("-@-"))
      res
    }
  }

  def removeConnectionInfoFromCause(cause: String): String = {
    val connectionSubStringStart = cause.indexOf(": ERROR:")
    cause.substring(connectionSubStringStart + 9)
  }

  def createRelationRowsImport(mutaction: CreateRelationRowsImport): SimpleDBIO[Vector[String]] = {
    val argsWithIndex: Seq[((IdGCValue, IdGCValue), Int)] = mutaction.args.zipWithIndex
    val relation                                          = mutaction.relation

    SimpleDBIO[Vector[String]] { x =>
      val res = try {
        val query = sql
          .insertInto(relationTable(relation))
          .columns(relationIdColumn(relation),
                   relationColumn(relation, relation.modelAField.relationSide),
                   relationColumn(relation, relation.modelBField.relationSide))
          .values(placeHolder, placeHolder, placeHolder)

        val relationInsert: PreparedStatement = x.connection.prepareStatement(query.getSQL)
        mutaction.args.foreach { arg =>
          relationInsert.setString(1, Cuid.createCuid())
          relationInsert.setGcValue(2, arg._1)
          relationInsert.setGcValue(3, arg._2)
          relationInsert.addBatch()
        }
        relationInsert.executeBatch()
        Vector.empty
      } catch {
        case e: java.sql.BatchUpdateException =>
          val faileds = e.getUpdateCounts.zipWithIndex

          faileds
            .filter(element => element._1 == Statement.EXECUTE_FAILED)
            .map { failed =>
              val failedA = argsWithIndex.find(_._2 == failed._2).get._1._1
              val failedB = argsWithIndex.find(_._2 == failed._2).get._1._2
              s"Failure inserting into relationtable ${relation.relationTableName} with ids $failedA and $failedB. Cause: ${removeConnectionInfoFromCause(e.getCause.toString)}"
            }
            .toVector
        case e: Exception => Vector(e.getMessage)
      }

      if (res.nonEmpty) throw new Exception(res.mkString("-@-"))
      res
    }
  }

  def pushScalarListsImport(mutaction: PushScalarListsImport)(implicit ec: ExecutionContext) = {
    val field   = mutaction.field
    val nodeIds = mutaction.valueTuples.keys

    for {
      startPositions <- startPositions(field, nodeIds.toSeq)
      // begin massage

      listValuesWithStartPosition: Iterable[(IdGCValue, ListGCValue, Int)] = {
        mutaction.valueTuples.map {
          case (id, listValue) => (id, listValue, startPositions.getOrElse(id, 0))
        }
      }

      individualValuesWithPosition: Iterable[(IdGCValue, GCValue, Int)] = listValuesWithStartPosition.flatMap {
        case (id, list, start) =>
          list.values.zipWithIndex.map { case (value, index) => (id, value, start + (index * 1000)) }
      }
      // end massage
      res <- importScalarListValues(field, individualValuesWithPosition)
    } yield res
  }

  private def startPositions(field: ScalarField, nodeIds: Seq[IdGCValue]): DBIO[Map[IdGCValue, Int]] = {
    val nodeIdField  = scalarListColumn(field, "nodeId")
    val placeholders = nodeIds.map(_ => placeHolder)

    val query = sql
      .select(nodeIdField, max(scalarListColumn(field, "position")))
      .from(scalarListTable(field))
      .groupBy(nodeIdField)
      .having(nodeIdField.in(placeholders: _*))

    val reads = ReadsResultSet { rs =>
      val nodeId = rs.getId(field.model, "nodeId")
      val start  = rs.getInt("max")
      (nodeId, start)
    }

    queryToDBIO(query)(
      setParams = pp => nodeIds.foreach(pp.setGcValue),
      readResult = rs => rs.as(reads).toMap
    )
  }

  private def importScalarListValues(field: ScalarField, values: Iterable[(IdGCValue, GCValue, Int)]): DBIO[Vector[String]] = SimpleDBIO { ctx =>
    val argsWithIndex: Iterable[((IdGCValue, GCValue, Int), Int)] = values.zipWithIndex

    val res = try {
      val query = sql
        .insertInto(scalarListTable(field))
        .columns(scalarListColumn(field, "nodeId"), scalarListColumn(field, "value"), scalarListColumn(field, "position"))
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

            s"Failure inserting into listTable ${field.model.dbName}_${field.dbName} for the id ${failedId.value} for value ${failedValue.value}. Cause: ${removeConnectionInfoFromCause(
              e.getCause.toString)}"
          }
          .toVector
      case e: Exception => Vector(e.getMessage)
    }
    if (res.nonEmpty) throw new Exception(res.mkString("-@-"))
    res
  }
}
